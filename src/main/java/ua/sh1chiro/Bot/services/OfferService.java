package ua.sh1chiro.Bot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.repositories.OfferRepository;

import java.util.List;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Service
@Slf4j
@RequiredArgsConstructor
public class OfferService {
    private final OfferRepository offerRepository;

    public Offer save(Offer offer){
        return offerRepository.save(offer);
    }

    public List<Offer> getAll(){
        return offerRepository.findAll();
    }

    public Offer getById(Long id){
        return offerRepository.findById(id).orElse(null);
    }

    public void delete (Offer offer){
        offerRepository.delete(offer);
    }

    public void deleteByOfferId(String offerId){
        offerRepository.deleteByOfferId(offerId);
    }
    public Offer getByOfferId(String offerId){
        return offerRepository.getByOfferId(offerId);
    }
    public Offer getByAssetId(String assetId){
        return offerRepository.getByAssetId(assetId);
    }

    public List<Offer> saveAll(List<Offer> offers){
        return offerRepository.saveAll(offers);
    }

}
