package ua.sh1chiro.Bot.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.User;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.utils.DMarket;

import java.io.IOException;
import java.util.List;

/**
 * Created by Sh1chiro on 22.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
public class test {
    @GetMapping("/test")
    public void test() throws IOException, InterruptedException {
        DMarket.getTargetsFromDMarket(BotConfig.config, "0");
    }
}
