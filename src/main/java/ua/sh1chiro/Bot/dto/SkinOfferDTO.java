package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Sh1chiro on 14.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SkinOfferDTO {
    private Long id;
    private String name;
    private String assetId;
    private double price;
    private double minPrice;
    private double maxPrice;
}
