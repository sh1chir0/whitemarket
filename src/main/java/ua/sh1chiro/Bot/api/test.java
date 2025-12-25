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
        WhiteMarket.setPartnerToken("8kcfheadktqofpsyrbaadjitnupzkcgxfyesmrmntect4l2nbg8t2bzm77fjmudd");
        WhiteMarket.authorize();

        var updated = WhiteMarket.editSellPriceUsd(
                "1f0e1c49-e196-6d6a-8f03-eab3b11af6d8",
                new BigDecimal("4.00")
        );
        System.out.println(updated);
    }
}