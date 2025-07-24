package ua.sh1chiro.Bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.sh1chiro.Bot.models.History;

/**
 * Created by Sh1chiro on 13.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public interface HistoryRepository extends JpaRepository<History, Long> {
}
