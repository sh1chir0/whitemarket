package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Created by Sh1chiro on 01.05.2025.
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
public class SkinPricesDTO {
    private int id;
    private String name;
    private double minWithLock;
    private double minWithoutLock;
    private double maxTarget;
    private String ownerWith;
    private String ownerWithout;

    @Override
    public String toString() {
        return "SkinPricesDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", minWithLock=" + minWithLock +
                ", minWithoutLock=" + minWithoutLock +
                ", maxTarget=" + maxTarget +
                ", ownerWith='" + ownerWith + '\'' +
                ", ownerWithout='" + ownerWithout + '\'' +
                '}';
    }
}
