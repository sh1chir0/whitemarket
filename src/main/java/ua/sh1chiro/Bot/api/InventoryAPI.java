package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ua.sh1chiro.Bot.dto.InventoryDMarketDTO;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.utils.WhiteMarket;

import java.util.List;

/**
 * Created by Sh1chiro on 25.12.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryAPI{
    @RequestMapping("/get-inventories")
    public List<InventoryDMarketDTO> getMyInventories(){
        List<InventoryDMarketDTO> inventoryDMarketDTOS = WhiteMarket.getAllTradeableInventoryCs2(100);
        for (InventoryDMarketDTO inventoryDMarketDTO : inventoryDMarketDTOS){
            inventoryDMarketDTO.setMinWM(WhiteMarket.getLowestSellPriceUsdCs2(inventoryDMarketDTO.getName()));
        }

        return inventoryDMarketDTOS;
    }
}
