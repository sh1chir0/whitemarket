package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Sh1chiro on 26.12.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EditBuyOrderResult {
    private String orderId;
    private String status;
    private double price;
    private String errorCategory;
    private String errorMessage;
}