package ua.sh1chiro.Bot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Created by Sh1chiro on 12.05.2025.
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
@Table(name = "targets")
@Entity
public class Target {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String targetId;
    private String assetId;
    private String name;
    private double price;
    private double minPrice;
    private double maxPrice;
    private double maxTarget;
    private double minWithoutLock;
    private double minWithLock;
    @Column(columnDefinition = "TEXT")
    private String imageLink;
    private LocalDateTime dateOfCreated;
    private LocalDateTime lastUpdateTime;

    @PrePersist
    private void init(){
        dateOfCreated = LocalDateTime.now();
    }
}
