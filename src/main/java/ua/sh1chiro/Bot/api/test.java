package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.User;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.utils.Competition;
import ua.sh1chiro.Bot.utils.DMarket;
import ua.sh1chiro.Bot.utils.WhiteMarket;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static ua.sh1chiro.Bot.utils.WhiteMarket.getAllDealsHistoryCs2;

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
@RequiredArgsConstructor
public class test {
    @GetMapping("/test")
    public void test(){
        System.out.println(getAllDealsHistoryCs2(null, 50, 10));
    }
}