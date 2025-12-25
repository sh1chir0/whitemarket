package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Sh1chiro on 16.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class InventoryDMarketDTO {
    private String assetId;
    private String name;
    private String imageLink;
    private String stickers;
    private double floatValue;
    private double price;
    private boolean tradable;
    private SkinPricesDTO skinPricesDTO;
    private double minWM;
    private String inventoryId;

    @Override
    public String toString() {
        return "InventoryDMarketDTO{" +
                "assetId='" + assetId + '\'' +
                ", name='" + name + '\'' +
                ", imageLink='" + imageLink + '\'' +
                ", stickers='" + stickers + '\'' +
                ", floatValue=" + floatValue +
                ", price=" + price +
                ", tradable=" + tradable +
                ", skinPricesDTO=" + skinPricesDTO +
                ", minWM=" + minWM +
                '}';
    }
}