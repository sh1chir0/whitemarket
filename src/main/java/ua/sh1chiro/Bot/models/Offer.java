package ua.sh1chiro.Bot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Created by Sh1chiro on 16.04.2025.
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
@Table(name = "offers")
@Entity
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String offerId;
    private String assetId;
    private String name;
    private double price;
    private double minPrice;
    private double maxPrice;
    private double minWithoutLock;
    private double minWithLock;
    private boolean tradable;
    private LocalDateTime dateOfCreated;
    private LocalDateTime lastUpdate;
    private LocalDateTime tryUpdate;

    @PrePersist
    private void init(){
        dateOfCreated = LocalDateTime.now();
        lastUpdate = LocalDateTime.now();
        tryUpdate = LocalDateTime.now();
    }
}
