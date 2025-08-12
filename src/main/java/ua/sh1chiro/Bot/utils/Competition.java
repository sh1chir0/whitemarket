package ua.sh1chiro.Bot.utils;

import org.springframework.stereotype.Component;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.OfferService;
import ua.sh1chiro.Bot.services.TargetService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Created by Sh1chiro on 12.05.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Component
public class Competition {
    private static OfferService offerService;
    private static TargetService targetService;
    public Competition(OfferService offerService, TargetService targetService) {
        Competition.offerService = offerService;
        Competition.targetService = targetService;
    }

    public static void offerCompetition(){
        int delay = BotConfig.config.getOfferDelay() * 60 * 1000;
        while(BotConfig.config.isCompetitionOffers()) {
            DMarket.updateOffers();
            try {
                Thread.sleep(2000);
                DMarket.updateOffersId();
            }catch (Exception ex){
                ex.printStackTrace();
            }

            List<Offer> offers = offerService.getAll();

            offers = DMarket.updateOfferTradable(offers);

            for (Offer offer : offers) {
                SkinPricesDTO skinPricesDTO = DMarket.getOffersBySkin(offer.getName());
                offer.setMinWithoutLock(skinPricesDTO.getMinWithoutLock());
                offer.setMinWithLock(skinPricesDTO.getMinWithLock());

                SkinPricesDTO skinPricesWithFrames = DMarket.getOffersBySkinWithFrames(offer.getName(), offer.getMinPrice(), offer.getMaxPrice());
                if(offer.isTradable()){
                    if(skinPricesWithFrames.getMinWithoutLock() != 0)
                        offer.setPrice(skinPricesWithFrames.getMinWithoutLock() - 0.01);
                    else
                        offer.setPrice(offer.getMaxPrice());
                }else {
                    if(skinPricesWithFrames.getMinWithLock() != 0)
                        offer.setPrice(skinPricesWithFrames.getMinWithLock() - 0.01);
                    else
                        offer.setPrice(offer.getMaxPrice());
                }

//                if (offer.isTradable() && (offer.getMinWithoutLock() - 0.01 >= offer.getMinPrice()) && (offer.getMinWithoutLock() - 0.01 <= offer.getMaxPrice()))
//                    offer.setPrice(offer.getMinWithoutLock() - 0.01);
//                else if ((offer.getMinWithLock() - 0.01 >= offer.getMinPrice()) && (offer.getMinWithLock() - 0.01 <= offer.getMaxPrice()))
//                    offer.setPrice(offer.getMinWithLock() - 0.01);

                offer.setTryUpdate(LocalDateTime.now());
                offerService.save(offer);

                try {
                    Thread.sleep(1000);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }


            DMarket.updateOfferPrice(offers);
            try {
                Thread.sleep(delay);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }

    public static void targetCompetition(){
        int delay = BotConfig.config.getTargetDelay()  * 60 * 1000;
        while (BotConfig.config.isCompetitionTargets()) {
            DMarket.updateTargetHistory();

            try {
                Thread.sleep(2000);
                DMarket.updateTargetsId();
            }catch (Exception ex){
                ex.printStackTrace();
            }

            List<Target> targets = targetService.getAllTargets();

            for (Target target : targets) {
                LocalDateTime time = LocalDateTime.now();
                if (target.getLastUpdateTime().isAfter(time.minusMinutes(15))) {
                    continue;
                }

                SkinPricesDTO skinPricesDTO = DMarket.getOffersBySkin(target.getName());
                target.setMinWithLock(skinPricesDTO.getMinWithLock());
                target.setMinWithoutLock(skinPricesDTO.getMinWithoutLock());

                try{
                    double maxTarget = DMarket.getMaxTargetWithoutAttributes(target.getName());
                    target.setMaxTarget(maxTarget);

                    List<Double> topPrices = DMarket.getTop2MaxTargetsWithoutAttributesWithFrames(target.getName(), target.getMinPrice(), target.getMaxPrice());
                    if(!topPrices.isEmpty())
                        if(target.getPrice() == topPrices.getFirst()){
                            if(topPrices.size() == 2)
                                target.setPrice(topPrices.getLast() + 0.01);
                        }else
                            target.setPrice(topPrices.getFirst() + 0.01);
                    else
                        target.setPrice(target.getMinPrice());
                }catch (Exception ex){
                    ex.printStackTrace();
                }
                targetService.save(target);

                try {
                    Thread.sleep(1000);
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }

            DMarket.updateTargets(targets);

            try{
                Thread.sleep(delay);
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }
    }
}
