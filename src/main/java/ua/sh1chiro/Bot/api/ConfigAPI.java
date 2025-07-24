package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.DelaysDTO;
import ua.sh1chiro.Bot.dto.KeysDTO;
import ua.sh1chiro.Bot.utils.Competition;

/**
 * Created by Sh1chiro on 12.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigAPI {

    @GetMapping("/competition-offers")
    public boolean competitionOffers(){
        return BotConfig.config.isCompetitionOffers();
    }

    @GetMapping("/competition-targets")
    public boolean competitionTargets(){
        return BotConfig.config.isCompetitionTargets();
    }

    @GetMapping("/change-offer-competition")
    public boolean changeOfferCompetition(){
        BotConfig.config.setCompetitionOffers(!BotConfig.config.isCompetitionOffers());
        BotConfig.saveConfig();

        if(BotConfig.config.isCompetitionOffers())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Competition.offerCompetition();
                }
            }).start();

        return BotConfig.config.isCompetitionOffers();
    }

    @GetMapping("/change-target-competition")
    public boolean changeTargetCompetition(){
        BotConfig.config.setCompetitionTargets(!BotConfig.config.isCompetitionTargets());
        BotConfig.saveConfig();

        if(BotConfig.config.isCompetitionTargets())
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Competition.targetCompetition();
                }
            }).start();

        return BotConfig.config.isCompetitionTargets();
    }

    @PostMapping("/change-keys")
    public ResponseEntity<String> changeKeys(@RequestBody KeysDTO keysDTO) {
        String publicKey = keysDTO.getPublicKey();
        String secretKey = keysDTO.getSecretKey();

        BotConfig.config.setPublicAPIKey(publicKey);
        BotConfig.config.setSecretAPIKey(secretKey);
        BotConfig.saveConfig();

        return ResponseEntity.ok("Keys updated");
    }

    @PostMapping("/change-delays")
    public ResponseEntity<String> changeDelays(@RequestBody DelaysDTO delaysDTO) {
        int offerDelay = delaysDTO.getOfferDelay();
        int targetDelay = delaysDTO.getTargetDelay();

        BotConfig.config.setOfferDelay(offerDelay);
        BotConfig.config.setTargetDelay(targetDelay);
        BotConfig.saveConfig();

        return ResponseEntity.ok("Keys updated");
    }

    @PostMapping("/login")
    public boolean login(@RequestBody String password){
        return BotConfig.config.getPassword().equals(password);
    }

    @PostMapping("/change-password")
    public ResponseEntity<String> changePassword(@RequestBody String password){
        BotConfig.config.setPassword(password);
        BotConfig.saveConfig();

        return ResponseEntity.ok("Password updated");    }
}
