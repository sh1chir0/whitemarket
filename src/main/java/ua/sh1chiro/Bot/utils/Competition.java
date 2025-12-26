package ua.sh1chiro.Bot.utils;

import org.springframework.stereotype.Component;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.EditBuyOrderResult;
import ua.sh1chiro.Bot.dto.SkinPricesDTO;
import ua.sh1chiro.Bot.dto.TargetPriceDTO;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.OfferService;
import ua.sh1chiro.Bot.services.TargetService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
    public static boolean offerCompetitionWorking;
    public static boolean targetCompetitionWorking;
    private static TargetService targetService;
    public Competition(OfferService offerService, TargetService targetService) {
        Competition.offerService = offerService;
        Competition.targetService = targetService;
    }

    public static void offerCompetition(){
        offerCompetitionWorking = true;

        int delay = BotConfig.config.getOfferDelay() * 60 * 1000;
        while(BotConfig.config.isCompetitionOffers()) {
            try {
                // Checking sold
                List<Offer> offers = offerService.getAll();

//                List<Offer> offersForDeleting = new ArrayList<>();

//                for (Offer offer : offers) {
//                    Optional<WhiteMarket.DealHistoryHit> hit = WhiteMarket.findDealInHistoryByProductId(offer.getProductId(), 200);
//                    if (hit.isPresent()) {
//                        offersForDeleting.add(offer);
//                    }
//                }
//                for (Offer offer : offersForDeleting) {
//                    offerService.delete(offer);
//                }
//
//                offers.removeAll(offersForDeleting);

                List<Offer> offersToUpdate = new ArrayList<>();
                System.out.println("Відбувається перевірка офферів на оновлення ціни: ");
                for (Offer offer : offers) {
                    WhiteMarket.LowestSell minWM = WhiteMarket.getLowestSellPriceWithIdUsdCs2(offer.getName());
                    System.out.println("Назва: " + offer.getName());
                    System.out.println("My product id: " + offer.getProductId());
                    System.out.println("MinWM price: " + minWM.priceUsd() + "$");
                    System.out.println("MinWM id: " + minWM.productId());

                    if(minWM.productId().equals(offer.getProductId()))
                        continue;

                    if(minWM.priceUsd() - 0.01 >= offer.getMinPrice() && minWM.priceUsd() - 0.01 <= offer.getMaxPrice()) {
                        offer.setPrice(minWM.priceUsd() - 0.01);
                        offer.setTryUpdate(LocalDateTime.now());

                        offersToUpdate.add(offer);
                        try {
                            Thread.sleep(1000);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }


                List<WhiteMarket.EditOfferResult> editOffers = WhiteMarket.editSellOffersPricesUsdCs2(offersToUpdate);
                for (WhiteMarket.EditOfferResult editOffer : editOffers) {
                    if(editOffer.errorMessage() == null){
                        for (Offer offer : offersToUpdate) {
                            if(offer.getProductId().equals(editOffer.productId())){
                                offer.setLastUpdate(LocalDateTime.now());
                                offerService.save(offer);
                            }
                        }
                    }
                }

                try {
                    Thread.sleep(delay);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        if (!BotConfig.config.isCompetitionOffers())
                            break;

                        Thread.sleep(delay);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }catch (Exception ex){
                try {
                    Thread.sleep(delay);
                }catch (Exception exi) {
                    ex.printStackTrace();
                }
                ex.printStackTrace();
            }
        }
        offerCompetitionWorking = false;

    }

    public static void targetCompetition(){
        targetCompetitionWorking = true;

        int delay = BotConfig.config.getTargetDelay()  * 60 * 1000;
        while (BotConfig.config.isCompetitionTargets()) {
            try {
                List<Target> targets = targetService.getAllTargets();

                for (Target target : targets) {
                    LocalDateTime time = LocalDateTime.now();
                    target.setLastTryUpdateTime(time);
                    targetService.save(target);

                    if (target.getLastUpdateTime().isAfter(time.minusMinutes(15))) {
                        continue;
                    }

                    try {
                        List<TargetPriceDTO> prices = WhiteMarket.getPublicBuyTargetsCs2(target.getName(), 2);
                        if(prices.getFirst().getOrderId().equals(target.getOrderId()))
                            continue;

                        double maxTarget = prices.getFirst().getPrice();
                        target.setMaxTarget(maxTarget);

                        if(maxTarget + 0.1 >= target.getMinPrice() && maxTarget + 0.1 <= target.getMaxPrice()){
                            target.setPrice(maxTarget + 0.1);
                        }else {
                            System.out.println("skip");
                            continue;
                        }

                        Thread.sleep(1000);

                        EditBuyOrderResult res =
                                WhiteMarket.editBuyOrderPriceUsdCs2(target.getOrderId(),maxTarget + 0.1);
                        if(res.getErrorMessage() == null) {
                            target.setLastUpdateTime(LocalDateTime.now());
                            targetService.save(target);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }


                try {
                    Thread.sleep(delay);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    try {
                        if (!BotConfig.config.isCompetitionTargets())
                            break;

                        Thread.sleep(delay);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }catch (Exception ex){
                try {
                    if (!BotConfig.config.isCompetitionTargets())
                        break;

                    Thread.sleep(delay);
                } catch (Exception exs) {
                    ex.printStackTrace();
                }
                ex.printStackTrace();
            }
        }

        targetCompetitionWorking = false;
    }
}
