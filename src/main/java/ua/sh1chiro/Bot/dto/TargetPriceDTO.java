package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Sh1chiro on 12.08.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class TargetPriceDTO{
    private String orderId;
    private double price;
    private int quantity;
    private String name;

    public TargetPriceDTO(String orderId, double price, int quantity) {
        this.orderId = orderId;
        this.price = price;
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "TargetPriceDTO{" +
                "orderId='" + orderId + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", name='" + name + '\'' +
                '}';
    }
}
