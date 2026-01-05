package ua.sh1chiro.Bot.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.sh1chiro.Bot.dto.*;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.TargetService;
import ua.sh1chiro.Bot.utils.DMarket;
import ua.sh1chiro.Bot.utils.WhiteMarket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * Created by Sh1chiro on 14.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
public class TargetAPI {
    private final TargetService targetService;

    private final Path uploadRoot = Paths.get("uploads");

    @PostMapping("/update-info")
    public List<SkinPricesDTO> updateInfo(@RequestBody List<String> names){
//        WhiteMarket.setPartnerToken("8kcfheadktqofpsyrbaadjitnupzkcgxfyesmrmntect4l2nbg8t2bzm77fjmudd");
//        WhiteMarket.authorize();

        List<SkinPricesDTO> skinPricesDTOS = new ArrayList<>();
        int i = 0;
        for (String name : names) {
            SkinPricesDTO skinPricesDTO = new SkinPricesDTO();
            skinPricesDTO.setName(name);
            skinPricesDTO.setId(i);

            LowestSellInfo lowestSellInfo = WhiteMarket.getLowestSellInfoUsdCs2(name);
            if(lowestSellInfo.imageLink() != null)
                skinPricesDTO.setImageLink(lowestSellInfo.imageLink());

            skinPricesDTO.setMinWM(lowestSellInfo.priceUsd());

            List<TargetPriceDTO> prices = WhiteMarket.getPublicBuyTargetsCs2InRange(name, 10, 0, 100000);
            if(!prices.isEmpty()) {
                skinPricesDTO.setMaxTarget(prices.getFirst().getPrice());
                skinPricesDTO.setTargets(prices);
            }else{
                skinPricesDTO.setMaxTarget(0);
                skinPricesDTO.setTargets(new ArrayList<>());
            }

            skinPricesDTOS.add(skinPricesDTO);
            System.out.println(skinPricesDTO.toString());
            i++;
        }

        return skinPricesDTOS;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createTargets(@RequestBody List<Target> targets) {
        targets.removeIf(t -> t == null || t.getPrice() == 0 || t.getMinPrice() == 0 || t.getMaxPrice() == 0);

        int saved = 0;

        for (Target t : targets) {
            try {
                WhiteMarket.Order o = WhiteMarket.createBuyTargetCs2(t.getName(), t.getPrice());
                t.setOrderId(o.id());
                t.setLastUpdateTime(LocalDateTime.now());
                targetService.save(t);
                saved++;
            } catch (Exception ex) {
                System.out.println("❌ Failed create target: " + t.getName() + " -> " + ex.getMessage());
            }
        }

        return ResponseEntity.ok("saved=" + saved + " / total=" + targets.size());
    }


    @GetMapping("/get-all")
    public ResponseEntity<List<Target>> getTargets(){
        List<Target> targets = targetService.getAllTargets();
        return ResponseEntity.ok(targets);
    }

    @GetMapping("/get-all-with-targets")
    public ResponseEntity<List<TargetWithSkinsDTO>> getTargetsWithMax(){
        List<Target> targets = targetService.getAllTargets();

        List<TargetWithSkinsDTO> result = targets.stream()
                .map(t -> {
                    List<TargetPriceDTO> skins = WhiteMarket.getPublicBuyTargetsCs2InRange(t.getName(), 10, 0 , 100000);

                    String myOrderId = t.getOrderId();

                    if (myOrderId != null && !myOrderId.isBlank()) {
                        skins.removeIf(s -> myOrderId.equals(s.getOrderId()));
                    }

                    if(skins.isEmpty())
                        skins = new ArrayList<>();

                    return new TargetWithSkinsDTO(
                            t.getId(),
                            t.getTargetId(),
                            t.getOrderId(),
                            t.getAssetId(),
                            t.getName(),
                            t.getPrice(),
                            t.getMinPrice(),
                            t.getMaxPrice(),
                            skins.isEmpty() ? 0 : skins.getFirst().getPrice(),
                            t.getMinWithoutLock(),
                            t.getMinWithLock(),
                            t.getImageLink(),
                            t.getDateOfCreated(),
                            t.getLastUpdateTime(),
                            skins
                    );
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/update")
    public ResponseEntity<String> update(@RequestBody List<Target> targets){
        List<Target> targetsForUpdate = targetService.getAllTargets();

        for (Target target : targets) {
            for (Target targetForSave : targetsForUpdate) {
                if(target.getId().equals(targetForSave.getId())){
                    targetForSave.setMaxPrice(target.getMaxPrice());
                    targetForSave.setMinPrice(target.getMinPrice());

                    targetService.save(targetForSave);
                }
            }
        }

        return ResponseEntity.ok("ok");
    }

    @GetMapping("/delete/{id}")
    public ResponseEntity<String> update(@PathVariable Long id){
        Target target = targetService.getById(id);
        if(target != null){
//            boolean result = DMarket.deleteTarget(target.getTargetId());
//            if(result)
                targetService.delete(target);
        }

        return ResponseEntity.ok("ok");
    }


    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadJsonFile(
            @RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Файл не передано або порожній");
            }

            try (InputStream is = file.getInputStream()) {
                List<TargetImportItem> items = objectMapper.readValue(
                        is, new TypeReference<List<TargetImportItem>>() {}
                );

                if (items == null || items.isEmpty()) {
                    return ResponseEntity.badRequest().body("JSON порожній або не масив");
                }

                List<Target> targets = new ArrayList<>(items.size());
                for (TargetImportItem it : items) {
                    targets.add(mapToTarget(it));
                }

                for (Target t : targets) {
                    try {
                        WhiteMarket.Order o = WhiteMarket.createBuyTargetCs2(t.getName(), t.getPrice());
                        t.setOrderId(o.id());
                        t.setLastUpdateTime(LocalDateTime.now());
                        targetService.save(t);
                    } catch (Exception ex) {
                        System.out.println("❌ Failed create target: " + t.getName() + " -> " + ex.getMessage());
                    }
                }

                return ResponseEntity.ok("Успішно оброблено: " + targets.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Помилка парсингу JSON: " + e.getMessage());
        }
    }

    private Target mapToTarget(TargetImportItem it) {
        Target t = new Target();
        t.setName(nvl(it.getHashName(), ""));
        t.setMinPrice(parseDouble(it.getMinPrice()));
        t.setMaxPrice(parseDouble(it.getMaxPrice()));
        t.setPrice(t.getMinPrice());
        t.setMaxTarget(it.getCount() != null ? it.getCount() : 0);

        SkinPricesDTO skinPricesDTO = DMarket.getOffersBySkin(t.getName());
        t.setMinWithoutLock(skinPricesDTO.getMinWithoutLock());
        t.setMinWithLock(skinPricesDTO.getMinWithLock());
        t.setImageLink(skinPricesDTO.getImageLink());
        t.setMaxTarget(DMarket.getMaxTargetWithoutAttributes(t.getName()));

        return t;
    }

    private static double parseDouble(String s) {
        if (s == null || s.isBlank()) return 0d;
        s = s.replace(',', '.').trim();
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0d; }
    }
    private static String nvl(String s, String def) { return (s == null) ? def : s; }
}