package ua.sh1chiro.Bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.sh1chiro.Bot.models.Config;
import ua.sh1chiro.Bot.repositories.ConfigRepository;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class ConfigService {
    private final ConfigRepository configRepository;

    public Config getConfig(){
        return configRepository.findFirstByOrderByIdAsc();
    }

    public Config save(Config config){
        return configRepository.save(config);
    }
}
