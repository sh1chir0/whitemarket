package ua.sh1chiro.Bot.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.services.OfferService;
import ua.sh1chiro.Bot.utils.WhiteMarket;

import java.util.*;

/**
 * Created by Sh1chiro on 30.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferAPI {
    private final OfferService offerService;

    @PostMapping("/create")
    public ResponseEntity<String> createOffers(@RequestBody List<Offer> offers){
        for (Offer offer : offers) {
            System.out.println(offer.toString());
        }
        var res = WhiteMarket.sellOffersCs2(offers);
        System.out.println(res);
        Set<Integer> removeIdx = new HashSet<>();

        for (WhiteMarket.SellResult r : res) {
            if (r == null) continue;

            int idx = r.offerIndex();
            if (idx < 0 || idx >= offers.size()) continue;

            if (r.errorMessage() != null && !r.errorMessage().isBlank()) {
                removeIdx.add(idx);
                continue;
            }

            if (r.productId() != null && !r.productId().isBlank()) {
                Offer offer = offers.get(idx);
                if (offer != null) {
                    offer.setProductId(r.productId());
                }
            }
        }

        List<Integer> sorted = new ArrayList<>(removeIdx);
        sorted.sort(Comparator.reverseOrder());
        for (int idx : sorted) {
            offers.remove(idx);
        }

        offerService.saveAll(offers);
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/get-all")
    public ResponseEntity<List<Offer>> getOffers(){
        List<Offer> offers = offerService.getAll();
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateOffers(@RequestBody List<Offer> offers){
        offers.removeIf(offer -> offer.getMaxPrice() == 0 || offer.getMinPrice() == 0);

        List<Offer> offersForUpdate = offerService.getAll();

        for (Offer offer : offers) {
            for (Offer offerForUpdate : offersForUpdate) {
                if(offer.getName().equals(offerForUpdate.getName())){
                    offerForUpdate.setMinPrice(offer.getMinPrice());
                    offerForUpdate.setMaxPrice(offer.getMaxPrice());

                    offerService.save(offerForUpdate);
                }
            }
        }
        
        return ResponseEntity.ok("ok");
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<String> deleteOffer(@PathVariable Long id){
        System.out.println(id);
//        List<Offer> offers = new ArrayList<>();
        Offer offer = offerService.getById(id);
        if(offer != null) {
//            System.out.println("offer != null");
//            System.out.println(offer.getOfferId());
//            offers.add(offer);
//        }
//        boolean result = DMarket.deleteOffer(offers);
//        if(result)
            offerService.delete(offer);
        }
        return ResponseEntity.ok("ok");
    }
}