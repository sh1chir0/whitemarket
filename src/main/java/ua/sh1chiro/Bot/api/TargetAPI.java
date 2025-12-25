package ua.sh1chiro.Bot.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.dto.TargetImportItem;
import ua.sh1chiro.Bot.dto.TargetPriceDTO;
import ua.sh1chiro.Bot.dto.TargetWithSkinsDTO;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.TargetService;
import ua.sh1chiro.Bot.utils.DMarket;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        List<SkinPricesDTO> skinPricesDTOS = new ArrayList<>();
        int i = 0;
        for (String name : names) {
            SkinPricesDTO skinPricesDTO = DMarket.getOffersBySkin(name.trim());
            skinPricesDTO.setName(name);
            skinPricesDTO.setId(i);
            skinPricesDTO.setMaxTarget(DMarket.getMaxTargetWithoutAttributes(name));
            skinPricesDTO.setTargets(DMarket.getTargetsForSkin(name));

            skinPricesDTOS.add(skinPricesDTO);
            i++;
        }

        return skinPricesDTOS;
    }

    @PostMapping("/create")
    public ResponseEntity<String> createTargets(@RequestBody List<Target> targets) throws IOException, InterruptedException {
        targets.removeIf(target -> target.getMaxPrice() == 0 || target.getMinPrice() == 0 || target.getPrice() == 0);

        for (Target target : targets) {
            System.out.println(target.toString());
        }

        DMarket.createTargets(targets);

        return ResponseEntity.ok("ok");
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
                    List<TargetPriceDTO> skins = DMarket.getTargetsForSkin(t.getName());

                    return new TargetWithSkinsDTO(
                            t.getId(),
                            t.getTargetId(),
                            t.getAssetId(),
                            t.getName(),
                            t.getPrice(),
                            t.getMinPrice(),
                            t.getMaxPrice(),
                            t.getMaxTarget(),
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

                DMarket.createTargets(targets);

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