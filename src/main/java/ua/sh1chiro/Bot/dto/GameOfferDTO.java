package ua.sh1chiro.Bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Created by Sh1chiro on 16.04.2025.
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
public class GameOfferDTO {
    @JsonProperty("Offers")
    private List<Object> offers;
}
