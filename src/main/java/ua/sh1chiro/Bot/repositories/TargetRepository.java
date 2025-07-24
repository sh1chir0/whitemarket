package ua.sh1chiro.Bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ua.sh1chiro.Bot.models.Target;

import java.util.List;

/**
 * Created by Sh1chiro on 13.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public interface TargetRepository extends JpaRepository<Target, Long> {
    List<Target> getByTargetId(String targetId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Target t WHERE t.targetId = :targetId")
    void deleteByTargetId(String targetId);

    List<Target> findTargetsByName(String name);

    Target findByName(String name);
}
