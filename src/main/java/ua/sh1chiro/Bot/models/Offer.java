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
    @Column(columnDefinition = "TEXT")
    private String imageLink;
    private LocalDateTime dateOfCreated;
    private LocalDateTime lastUpdate;
    private LocalDateTime tryUpdate;
    private double minWM;
    private String inventoryId;
    private String productId;

    @PrePersist
    private void init(){
        dateOfCreated = LocalDateTime.now();
        lastUpdate = LocalDateTime.now();
        tryUpdate = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Offer{" +
                "id=" + id +
                ", offerId='" + offerId + '\'' +
                ", assetId='" + assetId + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", minPrice=" + minPrice +
                ", maxPrice=" + maxPrice +
                ", minWithoutLock=" + minWithoutLock +
                ", minWithLock=" + minWithLock +
                ", tradable=" + tradable +
                ", imageLink='" + imageLink + '\'' +
                ", dateOfCreated=" + dateOfCreated +
                ", lastUpdate=" + lastUpdate +
                ", tryUpdate=" + tryUpdate +
                ", minWM=" + minWM +
                ", inventoryId='" + inventoryId + '\'' +
                ", productId='" + productId + '\'' +
                '}';
    }
}
