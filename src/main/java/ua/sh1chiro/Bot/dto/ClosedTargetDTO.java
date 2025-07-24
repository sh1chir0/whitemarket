package ua.sh1chiro.Bot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Sh1chiro on 13.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClosedTargetDTO {
    private String targetId;
    private String assetId;
    private String name;
    private String amount;
}
