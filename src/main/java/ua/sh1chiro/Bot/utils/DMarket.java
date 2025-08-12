package ua.sh1chiro.Bot.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.stereotype.Component;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.*;
import ua.sh1chiro.Bot.models.History;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Config;
import ua.sh1chiro.Bot.models.Target;
import ua.sh1chiro.Bot.services.HistoryService;
import ua.sh1chiro.Bot.services.OfferService;
import ua.sh1chiro.Bot.services.TargetService;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Sh1chiro on 14.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

@Slf4j
@Component
public class DMarket {
    private static OfferService offerService;
    private static TargetService targetService;
    private static HistoryService historyService;

    public DMarket(OfferService offerService, TargetService targetService, HistoryService historyService) {
        DMarket.offerService = offerService;
        DMarket.targetService = targetService;
        DMarket.historyService = historyService;
    }

    public static String getMe() throws IOException, InterruptedException {
        String endpoint = "/account/v1/user";
        HttpResponse<String> response = getRequest(endpoint,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey());

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

        return jsonObject.get("id").getAsString();
    }

    public static void updateTargets(List<Target> targets){
        for (Target target : targets) {
            LocalDateTime time = LocalDateTime.now();
            if (target.getLastUpdateTime().isAfter(time.minusMinutes(15))) {
                continue;
            }

            boolean result = deleteTarget(target.getTargetId());
            System.out.println("Target name: " + target.getName() + "\nDelete result: " + result);

            if(result) {
                List<Target> targetToCreate = new ArrayList<>();
                targetToCreate.add(target);
                try {
                    boolean resultCreating = createTargets(targetToCreate);

                    if(!resultCreating)
                        targetService.delete(target);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void updateTargetHistory() {
        List<Target> targets = targetService.getAllTargets();

        try {
            List<ClosedTargetDTO> closedTargetDTOS = getClosedUserTargets();
            Iterator<Target> iterator = targets.iterator();

            while (iterator.hasNext()) {
                Target target = iterator.next();
                for (ClosedTargetDTO closedTargetDTO : closedTargetDTOS) {
                    if (closedTargetDTO.getTargetId().equals(target.getTargetId())) {
                        iterator.remove();
                        targetService.delete(target);

                        History history = new History();
                        history.setPrice(closedTargetDTO.getAmount());
                        history.setName(closedTargetDTO.getName());
                        history.setOffer(false);
                        historyService.save(history);

                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void updateOffers(){
        List<Offer> offers = offerService.getAll();
        try {
            List<ClosedOfferDTO> closedOfferDTOS = getClosedUserOffers();

            Iterator<Offer> iterator = offers.iterator();
            while (iterator.hasNext()) {
                Offer offer = iterator.next();
                for (ClosedOfferDTO closedOfferDTO : closedOfferDTOS) {
                    if (closedOfferDTO.getOfferId().equals(offer.getOfferId())) {
                        iterator.remove();
                        offerService.delete(offer);

                        History history = new History();
                        history.setPrice(String.valueOf(offer.getPrice()));
                        history.setName(offer.getName());
                        history.setOffer(true);
                        historyService.save(history);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateOffersId(){
        List<Offer> offers = offerService.getAll();
        List<Offer> offersFromDMarket = getOffersFromDMarket(BotConfig.config, 0);
        for (Offer offerFromDMarket : offersFromDMarket) {
            for (Offer offer : offers) {
                if(offerFromDMarket.getAssetId().equals(offer.getAssetId())){
                    if(!offerFromDMarket.getOfferId().equals(offer.getOfferId())){
                        offer.setOfferId(offerFromDMarket.getOfferId());
                        offerService.save(offer);

                        System.out.println("Оффер ід апдейтнувся");
                    }
                }
            }
        }
    }

    public static void updateTargetsId(){
        List<Target> targets = targetService.getAllTargets();
        List<Target> targetsFromDMarket = getTargetsFromDMarket(BotConfig.config, 0);
        for (Target targetFromDMarket : targetsFromDMarket) {
            for (Target target : targets) {
                if(targetFromDMarket.getName().equals(target.getName())){
                    if(!targetFromDMarket.getTargetId().equals(target.getTargetId())){
                        target.setTargetId(targetFromDMarket.getTargetId());
                        targetService.save(target);

                        System.out.println("Таргет ід апдейтнувся");
                    }
                }
            }
        }
    }

    public static List<ClosedTargetDTO> getClosedUserTargets() throws IOException, InterruptedException {
        List<ClosedTargetDTO> closedUserTargets = new ArrayList<>();
        String endpoint = "/marketplace-api/v1/user-targets/closed";
        HttpResponse<String> response = getRequest(endpoint,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey());

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

        try {
            JsonArray items = jsonObject.getAsJsonArray("Trades");

            if(items != null && !items.isEmpty()) {
                for (JsonElement item : items) {
                    ClosedTargetDTO closedTargetDTO = new ClosedTargetDTO();
                    JsonObject closedTarget = item.getAsJsonObject();

                    closedTargetDTO.setTargetId(closedTarget.get("TargetID").getAsString());
                    closedTargetDTO.setAssetId(closedTarget.get("AssetID").getAsString());
                    closedTargetDTO.setName(closedTarget.get("Title").getAsString());
                    closedTargetDTO.setAmount(closedTarget.get("Price").getAsJsonObject().get("Amount").getAsString());

                    closedUserTargets.add(closedTargetDTO);
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return closedUserTargets;
    }

    public static List<ClosedOfferDTO> getClosedUserOffers() throws IOException, InterruptedException {
        List<ClosedOfferDTO> closedUserOffers = new ArrayList<>();
        String endpoint = "/marketplace-api/v1/user-offers/closed";
        HttpResponse<String> response = getRequest(endpoint,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey());

        System.out.println();
        System.out.println(response);
        System.out.println();
        System.out.println(response.body());
        System.out.println();

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
        try {
            JsonArray items = jsonObject.getAsJsonArray("Trades");

            for (JsonElement item : items) {
                JsonObject closedOffer = item.getAsJsonObject();

                ClosedOfferDTO closedOfferDTO = new ClosedOfferDTO();

                String targetId = closedOffer.get("OfferID").getAsString();

                closedOfferDTO.setOfferId(targetId);

                if (closedOffer.has("Fee")) {
                    JsonObject feeObject = closedOffer.get("Fee").getAsJsonObject();

                    JsonObject amountObject = feeObject.get("Amount").getAsJsonObject();

                    double fee = amountObject.get("Amount").getAsDouble();
                    closedOfferDTO.setFee(fee);
                }

                closedUserOffers.add(closedOfferDTO);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return closedUserOffers;
    }

    public static List<Offer> updateOfferTradable(List<Offer> offers){
        String endpointUserOffers = "/marketplace-api/v1/user-offers?limit=100&BasicFilters.PriceFrom=" + 0;

        HttpResponse<String> response = null;
        try {
            response = getRequest(endpointUserOffers, BotConfig.config.getPublicAPIKey(), BotConfig.config.getSecretAPIKey());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if(response != null) {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            System.out.println(response.body());

            try {
                if (jsonObject.has("Items")) {
                    JsonArray jsonArray = jsonObject.getAsJsonArray("Items");

                    if (!jsonArray.isEmpty()) {
                        for (JsonElement item : jsonArray) {
                            JsonObject offer = item.getAsJsonObject();

                            if (offer.has("AssetID")) {
                                String assetId = offer.get("AssetID").getAsString();
                                boolean tradable = offer.get("Tradable").getAsBoolean();

                                for (Offer offerForUpdate : offers) {
                                    if (offerForUpdate.getAssetId().equals(assetId))
                                        offerForUpdate.setTradable(tradable);
                                }
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return offers;
    }

    public static boolean updateOfferPrice(List<Offer> offers){
        String endpoint = "/marketplace-api/v1/user-offers/edit";
        try {
            String endpointForSubscribe = "/marketplace-api/v1/user-offers/edit" + ConverterToJSON.inventoryListToJson(offers);

            HttpResponse<String> response = postRequest(endpoint, endpointForSubscribe,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey(),
                    ConverterToJSON.inventoryListToJson(offers));

            System.out.println(response.body());
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

            if (jsonObject.has("Result")) {
                JsonArray updatedOffers = jsonObject.getAsJsonArray("Result");

                for (JsonElement updatedOffer : updatedOffers) {
                    JsonObject offer = updatedOffer.getAsJsonObject();

                    boolean successful = offer.get("Successful").getAsBoolean();
                    if (successful) {
                        JsonObject editedOffer = offer.get("EditOffer").getAsJsonObject();
                        String oldOfferId = editedOffer.get("OfferID").getAsString();

                        Offer offerForUpdate = offerService.getByOfferId(oldOfferId);

                        String newOfferId = offer.get("NewOfferID").getAsString();
                        offerForUpdate.setOfferId(newOfferId);

                        JsonObject priceObject = editedOffer.get("Price").getAsJsonObject();
                        double newPrice = priceObject.get("Amount").getAsDouble();

                        offerForUpdate.setPrice(newPrice);
                        offerForUpdate.setLastUpdate(LocalDateTime.now());

                        offerService.save(offerForUpdate);
                    }
                }

                return true;
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }

        return false;
    }

    public static List<Double> getTop2MaxTargetsWithoutAttributesWithFrames(String name, double minPrice, double maxPrice) {
        try {
            String endpoint = "/marketplace-api/v1/market-depth?gameId=a8db&title=" + URLEncoder.encode(name, "UTF-8");

            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray orders = jsonObject.getAsJsonArray("orders");
            System.out.println(orders);

            double max1 = -1;
            double max2 = -1;

            for (JsonElement element : orders) {
                JsonObject order = element.getAsJsonObject();
                JsonArray attributes = order.getAsJsonArray("attributes");

                if (attributes.isEmpty()) {
                    double price = order.get("price").getAsDouble() / 100;

                    if (price > maxPrice || price < minPrice) {
                        System.out.println("Skip: $" + price);
                        continue;
                    }

                    if (price > max1) {
                        max2 = max1;
                        max1 = price;
                    } else if (price > max2) {
                        max2 = price;
                    }
                }
            }

            List<Double> result = new ArrayList<>();
            if (max1 != -1) result.add(max1);
            if (max2 != -1) result.add(max2);

            return result;

        } catch (Exception ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    public static double getMaxTargetWithoutAttributesWithFrames(String name, double minPrice, double maxPrice) {
        try {
            String endpoint = "/marketplace-api/v1/market-depth?gameId=a8db&title=" + URLEncoder.encode(name, "UTF-8");

            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray orders = jsonObject.getAsJsonArray("orders");
//            System.out.println(orders);
            JsonObject maxTarget = null;
            double maxTargetPrice = -1;

            for (JsonElement element : orders) {
                JsonObject order = element.getAsJsonObject();
                JsonArray attributes = order.getAsJsonArray("attributes");

                if (attributes.isEmpty()) {
                    double price = order.get("price").getAsDouble()/100;

                    if(price > maxPrice || price < minPrice){
//                        System.out.println("Skip: $" + price);
                        continue;
                    }

                    if (price > maxTargetPrice) {
                        maxTargetPrice = price;
                        maxTarget = order;
                    }
                }
            }

            return maxTargetPrice;

        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static List<TargetPriceDTO> getTargetsForSkin(String name){
        List<TargetPriceDTO> targetPrices = new ArrayList<>();
        try {
            String endpoint = "/marketplace-api/v1/market-depth?gameId=a8db&title=" + URLEncoder.encode(name, "UTF-8");

            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray orders = jsonObject.getAsJsonArray("orders");
            System.out.println(orders);

            for (JsonElement element : orders) {
                JsonObject order = element.getAsJsonObject();
                JsonArray attributes = order.getAsJsonArray("attributes");

                if (attributes.isEmpty()) {
                    double price = order.get("price").getAsDouble() / 100;
                    int amount = order.get("amount").getAsInt();

                    TargetPriceDTO targetPriceDTO = new TargetPriceDTO();
                    targetPriceDTO.setPrice(price);
                    targetPriceDTO.setQuantity(amount);
                    targetPrices.add(targetPriceDTO);

                    if(targetPrices.size() == 10)
                        break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return targetPrices;
    }

    public static double getMaxTargetWithoutAttributes(String name) {
        try {
            String endpoint = "/marketplace-api/v1/market-depth?gameId=a8db&title=" + URLEncoder.encode(name, "UTF-8");

            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray orders = jsonObject.getAsJsonArray("orders");
//            System.out.println(orders);
            JsonObject maxTarget = null;
            double maxTargetPrice = -1;

            for (JsonElement element : orders) {
                JsonObject order = element.getAsJsonObject();
                JsonArray attributes = order.getAsJsonArray("attributes");

                if (attributes.isEmpty()) {
                    double price = order.get("price").getAsDouble()/100;

                    if (price > maxTargetPrice) {
                        maxTargetPrice = price;
                        maxTarget = order;
                    }
                }
            }

            return maxTargetPrice;

        } catch (Exception ex) {
            ex.printStackTrace();
            return -1;
        }
    }

    public static SkinPricesDTO getOffersBySkinWithFrames(String name, double minPrice, double maxPrice){
        try{
            String endpoint = "/exchange/v1/offers-by-title" + "?Title=" + URLEncoder.encode(name, "UTF-8");;
            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray objects = jsonObject.getAsJsonArray("objects");
            double minUnlocked = Double.MAX_VALUE;
            double minLocked = Double.MAX_VALUE;
            String ownerWith = "";
            String ownerWithout = "";
            String me = getMe();

            for (JsonElement elem : objects) {
                JsonObject item = elem.getAsJsonObject();

                String owner = item.get("owner").getAsString();
                if(me.equals(owner)) {
                    continue;
                }

                JsonObject extra = item.getAsJsonObject("extra");
                JsonObject price = item.getAsJsonObject("price");

                if (extra == null || price == null || !price.has("USD")) {
                    continue;
                }

                double usdPrice;

                try {
                    usdPrice = Double.parseDouble(price.get("USD").getAsString());
                } catch (NumberFormatException e) {
                    continue;
                }

                if(usdPrice/100 > maxPrice || usdPrice/100 < minPrice) {
//                    System.out.println("Скіпаємо: $" + usdPrice/100);
                    continue;
                }

                int tradeLockDuration = extra.get("tradeLockDuration").getAsInt();

                if (tradeLockDuration == 0 && usdPrice < minUnlocked) {
                    ownerWithout = owner;
                    minUnlocked = usdPrice;
                } else if (tradeLockDuration > 0 && usdPrice < minLocked) {
                    minLocked = usdPrice;
                    ownerWith = owner;
                }
            }
            SkinPricesDTO skinPricesDTO = new SkinPricesDTO();
            skinPricesDTO.setMinWithLock(minLocked == Double.MAX_VALUE ? 0 : minLocked/100);
            skinPricesDTO.setMinWithoutLock(minUnlocked == Double.MAX_VALUE ? 0 : minUnlocked/100);
            skinPricesDTO.setOwnerWith(ownerWith);
            skinPricesDTO.setOwnerWithout(ownerWithout);

            return skinPricesDTO;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new SkinPricesDTO();
    }

    public static SkinPricesDTO getOffersBySkin(String name){
        try{
            String endpoint = "/exchange/v1/offers-by-title" + "?Title=" + URLEncoder.encode(name, "UTF-8");;
            HttpResponse<String> response = getRequest(endpoint,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey());

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            JsonArray objects = jsonObject.getAsJsonArray("objects");
            double minUnlocked = Double.MAX_VALUE;
            double minLocked = Double.MAX_VALUE;
            String ownerWith = "";
            String ownerWithout = "";
            String image = "";

            String me = getMe();

            for (JsonElement elem : objects) {
                JsonObject item = elem.getAsJsonObject();

                String owner = item.get("owner").getAsString();
                if(me.equals(owner)) {
                    continue;
                }

                if(item.has("image")){
                    image = item.get("image").getAsString();
                }

                JsonObject extra = item.getAsJsonObject("extra");
                JsonObject price = item.getAsJsonObject("price");

                if (extra == null || price == null || !price.has("USD")) {
                    continue;
                }

                double usdPrice;
                try {
                    usdPrice = Double.parseDouble(price.get("USD").getAsString());
                } catch (NumberFormatException e) {
                    continue;
                }

                int tradeLockDuration = extra.get("tradeLockDuration").getAsInt();

                if (tradeLockDuration == 0 && usdPrice < minUnlocked) {
                    ownerWithout = owner;
                    minUnlocked = usdPrice;
                } else if (tradeLockDuration > 0 && usdPrice < minLocked) {
                    minLocked = usdPrice;
                    ownerWith = owner;
                }
            }
            SkinPricesDTO skinPricesDTO = new SkinPricesDTO();
            skinPricesDTO.setMinWithLock(minLocked == Double.MAX_VALUE ? 0 : minLocked/100);
            skinPricesDTO.setMinWithoutLock(minUnlocked == Double.MAX_VALUE ? 0 : minUnlocked/100);
            skinPricesDTO.setOwnerWith(ownerWith);
            skinPricesDTO.setOwnerWithout(ownerWithout);
            skinPricesDTO.setImageLink(image);

            return skinPricesDTO;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return new SkinPricesDTO();
    }

    public static double getUserBalance(Config user) throws IOException, InterruptedException {
        double balance = -1;

        if(user != null){
            try {
                String endpoint = "/account/v1/balance";

                HttpResponse<String> response = getRequest(endpoint,
                        user.getPublicAPIKey(),
                        user.getSecretAPIKey());

                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

                System.out.println(jsonObject);
                if (jsonObject.has("usd")) {
                    balance = jsonObject.get("usd").getAsDouble() / 100;
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return balance;
    }

    public static boolean deleteOffer(List<Offer> offers) throws IOException, InterruptedException {
        String endpoint = "/exchange/v1/offers";
        System.out.println(ConverterToJSON.offersForDeleteToJson(offers));
        String endpointForSubscribe = "/exchange/v1/offers" + ConverterToJSON.offersForDeleteToJson(offers);

        HttpResponse<String> response = deleteRequest(endpoint, endpointForSubscribe,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey(),
                ConverterToJSON.offersForDeleteToJson(offers));
        try {
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            System.out.println(jsonObject);
            if (jsonObject.has("success")) {
                JsonArray jsonArray = jsonObject.get("success").getAsJsonArray();
                System.out.println("true");
                return true;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return false;
    }

    public static boolean deleteTarget(String targetId){
        String endpoint = "/marketplace-api/v1/user-targets/delete";
        String endpointForSubscribe = "/marketplace-api/v1/user-targets/delete" + ConverterToJSON.targetIdToJson(targetId);

        HttpResponse<String> response = null;
        try {
            response = postRequest(endpoint, endpointForSubscribe,
                    BotConfig.config.getPublicAPIKey(),
                    BotConfig.config.getSecretAPIKey(),
                    ConverterToJSON.targetIdToJson(targetId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if(response != null) {
            try {
                Gson gson = new Gson();
                JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
                System.out.println(jsonObject);
                if (jsonObject.has("Result")) {
                    JsonArray items = jsonObject.getAsJsonArray("Result");
                    for (JsonElement item : items) {
                        JsonObject targetResult = item.getAsJsonObject();

                        boolean successful = targetResult.get("Successful").getAsBoolean();

                        if (successful)
                            return true;
                        else {
                            JsonObject targetError = targetResult.get("Error").getAsJsonObject();
                            if (targetError.has("Message")) {
                                String message = targetError.get("Message").getAsString();

                                return false;
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static List<InventoryDMarketDTO> getUserInventory(Config user) throws IOException, InterruptedException {
        try{
        List<InventoryDMarketDTO> inventoryDMarketDTOS = new ArrayList<>();

        String endpoint = "/marketplace-api/v1/user-inventory?gameId=a8db&BasicFilters.InMarket=true&Limit=1000";

        HttpResponse<String> response = getRequest(endpoint,
                user.getPublicAPIKey(),
                user.getSecretAPIKey());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

        if(jsonObject.has("Items")) {
            JsonArray items = jsonObject.getAsJsonArray("Items");
            try {
                if (!items.isEmpty()) {
                    inventoryDMarketDTOS = parseInventories(items);

                    if (jsonObject.has("Cursor")) {
                        String cursor = jsonObject.get("Cursor").getAsString();

                        int i = 0;
                        while (true) {
                            if (!cursor.isEmpty()) {
                                String endpointWithCursor = "/marketplace-api/v1/user-inventory?gameId=a8db&BasicFilters.InMarket=true&Limit=1000&Cursor=" + cursor;

                                HttpResponse<String> responseWithCursor = getRequest(endpointWithCursor,
                                        user.getPublicAPIKey(),
                                        user.getSecretAPIKey());

                                Gson gsonWithCursor = new Gson();
                                JsonObject jsonObjectWithCursor = gsonWithCursor.fromJson(responseWithCursor.body(), JsonObject.class);

                                if(jsonObjectWithCursor.has("Items")) {
                                    JsonArray itemsWithCursor = jsonObjectWithCursor.getAsJsonArray("Items");

                                    inventoryDMarketDTOS.addAll(parseInventories(itemsWithCursor));

                                    if (jsonObjectWithCursor.has("Cursor")) {
                                        cursor = jsonObjectWithCursor.get("Cursor").getAsString();
                                        if (cursor.isEmpty())
                                            break;
                                    } else
                                        break;
                                }else{
                                    break;
                                }
                            } else
                                break;

                            i++;
                        }
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }else{
            response = getRequest(endpoint,
                    user.getPublicAPIKey(),
                    user.getSecretAPIKey());

            jsonObject = gson.fromJson(response.body(), JsonObject.class);

            if(jsonObject.has("Items")) {
                JsonArray items = jsonObject.getAsJsonArray("Items");
                try {
                    if (!items.isEmpty()) {
                        inventoryDMarketDTOS = parseInventories(items);

                        if (jsonObject.has("Cursor")) {
                            String cursor = jsonObject.get("Cursor").getAsString();

                            int i = 0;
                            while (true) {
                                if (!cursor.isEmpty()) {
                                    String endpointWithCursor = "/marketplace-api/v1/user-inventory?gameId=a8db&BasicFilters.InMarket=true&Limit=1000&Cursor=" + cursor;

                                    HttpResponse<String> responseWithCursor = getRequest(endpointWithCursor,
                                            user.getPublicAPIKey(),
                                            user.getSecretAPIKey());

                                    Gson gsonWithCursor = new Gson();
                                    JsonObject jsonObjectWithCursor = gsonWithCursor.fromJson(responseWithCursor.body(), JsonObject.class);

                                    if (jsonObjectWithCursor.has("Items")) {
                                        JsonArray itemsWithCursor = jsonObjectWithCursor.getAsJsonArray("Items");

                                        inventoryDMarketDTOS.addAll(parseInventories(itemsWithCursor));

                                        if (jsonObjectWithCursor.has("Cursor")) {
                                            cursor = jsonObjectWithCursor.get("Cursor").getAsString();
                                            if (cursor.isEmpty())
                                                break;
                                        } else
                                            break;
                                    } else {
                                        break;
                                    }
                                } else
                                    break;

                                i++;
                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        return inventoryDMarketDTOS;

        }catch (Exception ex){
            ex.printStackTrace();
            return null;
        }
    }

    private static List<InventoryDMarketDTO> parseInventories(JsonArray items){
        List<InventoryDMarketDTO> inventoryDMarketDTOS = new ArrayList<>();
        try {
            for (JsonElement item : items) {
                JsonObject skin = item.getAsJsonObject();

                InventoryDMarketDTO inventoryDMarketDTO = new InventoryDMarketDTO();

                inventoryDMarketDTO.setAssetId(skin.get("AssetID").getAsString());
                inventoryDMarketDTO.setName(skin.get("Title").getAsString());
                inventoryDMarketDTO.setImageLink(skin.get("ImageURL").getAsString());
                inventoryDMarketDTO.setTradable(skin.get("Tradable").getAsBoolean());
                if(skin.has("Attributes")){
                    JsonArray attributesArray = skin.getAsJsonArray("Attributes");

                    if(!attributesArray.isEmpty()){
                        for (JsonElement jsonElement : attributesArray) {
                            JsonObject attributeElement = jsonElement.getAsJsonObject();

                            if(attributeElement.has("Name")){
                                String attributeName = attributeElement.get("Name").getAsString();

                                if(attributeName.equals("stickersIdentical")){
                                    String value = attributeElement.get("Value").getAsString();
                                    Pattern pattern = Pattern.compile("name:(.*?)\\]");

                                    Matcher matcher = pattern.matcher(value);

                                    String stickers = "";
                                    List<String> stickerList = new ArrayList<>();
                                    while (matcher.find()) {
                                        String sticker = matcher.group().replace("name:", "").replace("]", "");
                                        stickerList.add(sticker);
                                    }

                                    if(!stickerList.isEmpty()) {
                                        stickers = String.join(";", stickerList);
                                        inventoryDMarketDTO.setStickers(stickers);
                                    }
                                }else if(attributeName.equals("floatValue")){
                                    double floatValue = attributeElement.get("Value").getAsDouble();

                                    inventoryDMarketDTO.setFloatValue(floatValue);
                                }
                            }
                        }
                    }
                }

                if(skin.has("Offer")){
                    JsonObject offerObject = skin.getAsJsonObject("Offer");

                    if(offerObject.has("Price")){
                        JsonObject priceObject = offerObject.getAsJsonObject("Price");

                        inventoryDMarketDTO.setPrice(priceObject.get("Amount").getAsDouble());

//                        bot.sendMessageToChat("$" + inventoryDMarketDTO.getPrice(), BotConfig.telegramConfig.getChatId());
                    }
                }

                inventoryDMarketDTOS.add(inventoryDMarketDTO);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return inventoryDMarketDTOS;
    }

    public static void createOffers(Config user, List<Offer> offers) throws IOException, InterruptedException {
        List<SkinOfferDTO> data = ConverterToJSON.offerToSkinOfferDTO(offers);

        String endpoint = "/marketplace-api/v1/user-offers/create";
        String endpointForSubscribe = "/marketplace-api/v1/user-offers/create" + ConverterToJSON.skinOfferToJson(data);

        HttpResponse<String> response = postRequest(endpoint, endpointForSubscribe,
                user.getPublicAPIKey(),
                user.getSecretAPIKey(),
                ConverterToJSON.skinOfferToJson(data));


        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
        try {
            if (jsonObject.has("Result")) {

                JsonArray jsonArray = jsonObject.getAsJsonArray("Result");


                for (JsonElement jsonElement : jsonArray) {
                    JsonObject offer = jsonElement.getAsJsonObject();

                    boolean result = offer.get("Successful").getAsBoolean();

                    if(result){
                        for (Offer offerForSave : offers) {
                            if(offer.get("CreateOffer").getAsJsonObject().get("AssetID").getAsString().equals(offerForSave.getAssetId())) {
                                offerForSave.setOfferId(offer.get("OfferID").getAsString());

                                offerService.save(offerForSave);
                            }
                        }
                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public static List<Target> getTargetsFromDMarket(Config user, int money){
        String endpointUserTargets = "/marketplace-api/v1/user-targets?limit=100&BasicFilters.Status=TargetStatusActive&BasicFilters.PriceFrom=" + money;
        List<Target> targets = new ArrayList<>();
        try{
            HttpResponse<String> response = getRequest(endpointUserTargets, user.getPublicAPIKey(), user.getSecretAPIKey());
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
            System.out.println(jsonObject);
            String result = "Не знайдено таргетів.";

                if(jsonObject.has("Items")){
                    JsonArray jsonArray = jsonObject.get("Items").getAsJsonArray();

                    if(!jsonArray.isEmpty()) {
                        for (JsonElement jsonElement : jsonArray) {
                            JsonObject target = jsonElement.getAsJsonObject();
                            if (target.has("Title")) {
                                String title = target.get("Title").getAsString();
                                result = result.concat("\n" + title);

                                Target targetForSave = new Target();

                                if (target.has("TargetID")) {
                                    String targetId = target.get("TargetID").getAsString();
                                    result = result.concat("\nTargetID: " + targetId);

                                    targetForSave.setTargetId(targetId);
                                    targetForSave.setName(title);
                                    targets.add(targetForSave);
                                }
                                if (target.has("Amount")) {
                                    String amount = target.get("Amount").getAsString();
                                    result = result.concat("\nAmount: " + amount);
                                }
                                if (target.has("Price")) {
                                    JsonObject priceObject = target.get("Price").getAsJsonObject();
                                    String price = priceObject.get("Amount").getAsString();
                                    result = result.concat("\nPrice: $" + price);

                                }
                                if (target.has("Status")) {
                                    String status = target.get("Status").getAsString();
                                    result = result.concat("\nStatus: " + status + "\n__________________");
                                }
                            }
                        }
                    }
                }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return targets;
    }
    public static List<Offer> getOffersFromDMarket(Config user, int money){
        String endpointUserOffers = "/marketplace-api/v1/user-offers?limit=100&BasicFilters.PriceFrom=" + money;
        List<Offer> offers = new ArrayList<>();
        try{
        HttpResponse<String> response = getRequest(endpointUserOffers, user.getPublicAPIKey(), user.getSecretAPIKey());
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
        System.out.println(response.body());
        String result = "Не знайдено офферів.";

            if(jsonObject.has("Items")){
                JsonArray jsonArray = jsonObject.getAsJsonArray("Items");

                if(!jsonArray.isEmpty()) {
                    int count = 0;
                    for (JsonElement item : jsonArray) {
                        JsonObject offer = item.getAsJsonObject();
                        if (offer.has("Title")) {
                            String title = offer.get("Title").getAsString();
                            result = result.concat("\n" + title);
                            if (offer.has("AssetID")) {
                                String assetId = offer.get("AssetID").getAsString();
                                result = result.concat("\nAssetID:\n" + assetId);

                                if (offer.has("Offer")) {
                                    JsonObject offerBlock = offer.get("Offer").getAsJsonObject();

                                    if (offerBlock.has("OfferID")) {
                                        String offerId = offerBlock.get("OfferID").getAsString();

                                        result = result.concat("\nOfferID:\n" + offerId);

                                        JsonObject priceBlock = offerBlock.get("Price").getAsJsonObject();
                                        if (priceBlock.has("Amount")) {
                                            String amount = priceBlock.get("Amount").getAsString();

                                            result = result.concat("\nPrice: $" + amount + "\n________________");

                                            count++;

                                            Offer offerToSave = new Offer();
                                            offerToSave.setOfferId(offerId);
                                            offerToSave.setAssetId(assetId);
                                            offers.add(offerToSave);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
        return offers;
    }
    public static boolean createTargets(List<Target> targets) throws IOException, InterruptedException {
        List<SkinTargetDTO> data = ConverterToJSON.targetsToSkinTargetDTO(targets);
        String endpoint = "/marketplace-api/v1/user-targets/create";
        String endpointForSubscribe = "/marketplace-api/v1/user-targets/create" + ConverterToJSON.skinTargetToJson(data);

        HttpResponse<String> response = postRequest(endpoint, endpointForSubscribe,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey(),
                ConverterToJSON.skinTargetToJson(data));

        String responseToUser = "";

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
        boolean successful = false;
        try {
            if (jsonObject.has("Result")) {
                JsonArray items = jsonObject.getAsJsonArray("Result");

                try {
                    for (JsonElement sale : items) {
                        JsonObject targetInfo = sale.getAsJsonObject();

                        String targetId = targetInfo.get("TargetID").getAsString();

                        JsonObject createdTarget = targetInfo.getAsJsonObject("CreateTarget");

                        String title = createdTarget.get("Title").getAsString();

                        successful = targetInfo.get("Successful").getAsBoolean();

                        if (successful) {
                            for (Target target : targets) {
                                if (target.getName().equals(title)) {
                                    target.setTargetId(targetId);
                                    target.setLastUpdateTime(LocalDateTime.now());
                                    targetService.save(target);
                                }
                            }
                        }

                        String errorMessage = "";
                        try {
                            JsonObject error = targetInfo.getAsJsonObject("Error");

                            errorMessage = error.get("Message").getAsString();
                        } catch (Exception ex) {
                        }
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }

                return successful;
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return false;
    }

    public static double getMaxTarget(String name) throws IOException, InterruptedException {
        String endpoint = "/price-aggregator/v1/aggregated-prices?Titles=" + URLEncoder.encode(name, "UTF-8");

        HttpResponse<String> response = getRequest(endpoint,
                BotConfig.config.getPublicAPIKey(),
                BotConfig.config.getSecretAPIKey());

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);
//        System.out.println(jsonObject);
        double maxTarget = -1;

        try {
            JsonArray items = jsonObject.getAsJsonArray("AggregatedTitles");

            for (JsonElement sale : items) {
                JsonObject saleInfo = sale.getAsJsonObject();

                JsonObject orders = saleInfo.getAsJsonObject("Orders");
                maxTarget = orders.get("BestPrice").getAsDouble();
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return maxTarget;
    }

    private static HttpResponse<String> deleteRequest(String endpoint, String endpointForSign,
                                                         String API_KEY, String SECRET_KEY, String jsonData) throws IOException, InterruptedException {

        Instant now = Instant.now();
        long timestamp = now.getEpochSecond();
        String timestampStr = String.valueOf(timestamp);

        String url = "https://api.dmarket.com" + endpoint;
        String signature = generateSignature("DELETE", endpointForSign, timestampStr, SECRET_KEY);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", API_KEY)
                .header("X-Sign-Date", timestampStr)
                .header("X-Request-Sign", "dmar ed25519 " + signature)
                .method("DELETE", HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private static HttpResponse<String> postRequest(String endpoint, String endpointForSign,
                                                       String API_KEY, String SECRET_KEY, String jsonData) throws IOException, InterruptedException {
        Instant now = Instant.now();
        long timestamp = now.getEpochSecond();
        String timestampStr = String.valueOf(timestamp);

        String url = "https://api.dmarket.com" + endpoint;
        String signature = generateSignature("POST", endpointForSign, timestampStr, SECRET_KEY);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("X-Api-Key", API_KEY)
                .header("X-Sign-Date", timestampStr)
                .header("X-Request-Sign", "dmar ed25519 " + signature)
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private static HttpResponse<String> getRequest(String endpoint, String API_KEY, String SECRET_KEY) throws IOException, InterruptedException {
        Instant now = Instant.now();
        long timestamp = now.getEpochSecond();
        String timestampStr = String.valueOf(timestamp);

        String url = "https://api.dmarket.com" + endpoint;
        String signature = generateSignature("GET", endpoint, timestampStr, SECRET_KEY);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Api-Key", API_KEY)
                .header("X-Sign-Date", timestampStr)
                .header("X-Request-Sign", "dmar ed25519 " + signature)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response;
    }

    private static String generateSignature(String method, String path, String timestamp, String SECRET_KEY){
        String nonSignedString = method + path + timestamp;

        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(hexStringToByteArray(SECRET_KEY), 0);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);

        byte[] message = nonSignedString.getBytes(StandardCharsets.UTF_8);
        signer.update(message, 0, message.length);

        byte[] digitalSignature = signer.generateSignature();

        String signatureString = bytesToHex(digitalSignature);
        return signatureString;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    private static byte[] hexStringToByteArray(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }
}
