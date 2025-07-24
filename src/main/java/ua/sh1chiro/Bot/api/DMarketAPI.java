package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.InventoryDMarketDTO;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.utils.DMarket;

import java.io.IOException;
import java.util.List;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/dmarket")
@RequiredArgsConstructor
public class DMarketAPI {

    @RequestMapping("/get-inventories")
    public List<InventoryDMarketDTO> getMyInventories() throws IOException, InterruptedException {
        List<InventoryDMarketDTO> inventoryDMarketDTOS = DMarket.getUserInventory(BotConfig.config);
        for (InventoryDMarketDTO inventoryDMarketDTO : inventoryDMarketDTOS) {
            SkinPricesDTO skinPricesDTO = DMarket.getOffersBySkin(inventoryDMarketDTO.getName());
            inventoryDMarketDTO.setSkinPricesDTO(skinPricesDTO);
        }

        return inventoryDMarketDTOS;
    }
}
