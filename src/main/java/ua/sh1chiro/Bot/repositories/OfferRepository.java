package ua.sh1chiro.Bot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ua.sh1chiro.Bot.models.Offer;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public interface OfferRepository extends JpaRepository<Offer, Long> {
    @Transactional
    @Modifying
    @Query("DELETE FROM Offer o WHERE o.offerId = :offerId")
    void deleteByOfferId(String offerId);

    @Transactional
    @Modifying
    @Query("DELETE FROM Offer o WHERE o.assetId = :assetId")
    void deleteByAssetId(String assetId);

    Offer getByOfferId(String offerId);
    Offer getByAssetId(String assetId);
}
