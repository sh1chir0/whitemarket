package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by Sh1chiro on 23.08.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TargetWithSkinsDTO {
    private Long id;
    private String targetId;
    private String orderId;
    private String assetId;
    private String name;
    private double price;
    private double minPrice;
    private double maxPrice;
    private double maxTarget;
    private double minWithoutLock;
    private double minWithLock;
    private String imageLink;
    private LocalDateTime dateOfCreated;
    private LocalDateTime lastUpdateTime;
    private List<TargetPriceDTO> targets;
}
