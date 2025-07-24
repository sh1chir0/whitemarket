package ua.sh1chiro.Bot.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ua.sh1chiro.Bot.models.Config;
import ua.sh1chiro.Bot.services.ConfigService;
import ua.sh1chiro.Bot.utils.Competition;

import java.io.IOException;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Component
public class BotConfig {
    private static ConfigService configService;
    public static Config config;

    public BotConfig(ConfigService configService) {
        BotConfig.configService = configService;
    }

    @PostConstruct
    private static void updateConfig(){
        config = configService.getConfig();
    }

    public static void saveConfig(){
        configService.save(config);
    }
}
