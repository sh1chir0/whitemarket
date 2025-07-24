package ua.sh1chiro.Bot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Sh1chiro on 14.04.2025.
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
@Table(name = "configs")
@Entity
public class Config {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(columnDefinition = "TEXT")
    private String publicAPIKey;
    @Column(columnDefinition = "TEXT")
    private String secretAPIKey;
    private boolean competitionOffers;
    private boolean competitionTargets;
    private String password;
    private int offerDelay = 5;
    private int targetDelay = 20;
}