package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.TargetService;
import ua.sh1chiro.Bot.utils.DMarket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}