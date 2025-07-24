package ua.sh1chiro.Bot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ua.sh1chiro.Bot.dto.*;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Sh1chiro on 14.04.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public class ConverterToJSON {
    public static String skinTargetToJson(List<SkinTargetDTO> data) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        GameTargetDTO gameTargetDTO = new GameTargetDTO();
        gameTargetDTO.setGameID("a8db");

        List<Object> targets = new ArrayList<>();

        for (SkinTargetDTO skinTarget : data) {
            ObjectNode targetNode = objectMapper.createObjectNode();
            targetNode.put("Amount", String.valueOf(skinTarget.getQuantity()));
            ObjectNode priceNode = objectMapper.createObjectNode();
            priceNode.put("Currency", "USD");
            priceNode.put("Amount", skinTarget.getPrice());
            targetNode.set("Price", priceNode);
            targetNode.put("Title", skinTarget.getName());
            targets.add(targetNode);
        }

        gameTargetDTO.setTargets(targets);

        String json = objectMapper.writeValueAsString(gameTargetDTO);

        return json;
    }

    public static String skinOfferToJson(List<SkinOfferDTO> data) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        GameOfferDTO gameOfferDTO = new GameOfferDTO();

        List<Object> offers = new ArrayList<>();

        for (SkinOfferDTO skinOffer : data) {
            ObjectNode offerNode = objectMapper.createObjectNode();
            offerNode.put("AssetID", skinOffer.getAssetId());
            ObjectNode priceNode = objectMapper.createObjectNode();
            priceNode.put("Currency", "USD");
            priceNode.put("Amount", skinOffer.getPrice());
            offerNode.set("Price", priceNode);
            offers.add(offerNode);
        }

        gameOfferDTO.setOffers(offers);

        String json = objectMapper.writeValueAsString(gameOfferDTO);

        return json;
    }

    public static String targetIdToJson(String targetId){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();

        ArrayNode targetsArray = objectMapper.createArrayNode();
        ObjectNode targetNode = objectMapper.createObjectNode();
        targetNode.put("TargetID", targetId);
        targetsArray.add(targetNode);

        rootNode.set("Targets", targetsArray);

        try {
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String offersForDeleteToJson(List<Offer> offers) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("force", true);

        ArrayNode objectsArray = objectMapper.createArrayNode();

        for (Offer offer : offers) {
            ObjectNode offerNode = objectMapper.createObjectNode();
            offerNode.put("itemId", offer.getAssetId());
            offerNode.put("offerId", offer.getOfferId());

            ObjectNode priceNode = objectMapper.createObjectNode();
            priceNode.put("amount", String.valueOf(offer.getPrice()));
            priceNode.put("currency", "USD");

            offerNode.set("price", priceNode);
            objectsArray.add(offerNode);
        }

        rootNode.set("objects", objectsArray);

        try {
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static List<SkinOfferDTO> offerToSkinOfferDTO(List<Offer> offers){
        List<SkinOfferDTO> skinOfferDTOS = new ArrayList<>();
        for (Offer offer : offers) {
            SkinOfferDTO skinOfferDTO = new SkinOfferDTO();
            skinOfferDTO.setId(offer.getId());
            skinOfferDTO.setName(offer.getName());
            skinOfferDTO.setAssetId(offer.getAssetId());
            skinOfferDTO.setPrice(offer.getPrice());
            skinOfferDTO.setMaxPrice(offer.getMaxPrice());
            skinOfferDTO.setMinPrice(offer.getMinPrice());

            skinOfferDTOS.add(skinOfferDTO);
        }

        return skinOfferDTOS;
    }

    public static List<SkinTargetDTO> targetsToSkinTargetDTO(List<Target> targets){
        List<SkinTargetDTO> skinTargetDTOS = new ArrayList<>();

        for (Target target : targets) {
            SkinTargetDTO skinTargetDTO = new SkinTargetDTO();

            skinTargetDTO.setQuantity(1);
            skinTargetDTO.setName(target.getName().trim());
            skinTargetDTO.setPrice(target.getPrice());

            skinTargetDTOS.add(skinTargetDTO);
        }

        return skinTargetDTOS;
    }

    public static String inventoryListToJson(List<Offer> inventories) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        OfferUpdateDTO offerUpdateDTO = new OfferUpdateDTO();

        List<Object> offers = new ArrayList<>();

        for (Offer inventory : inventories) {
            ObjectNode offerNode = objectMapper.createObjectNode();
            offerNode.put("OfferID", inventory.getOfferId());
            offerNode.put("AssetID", inventory.getAssetId());
            ObjectNode priceNode = objectMapper.createObjectNode();
            priceNode.put("Currency", "USD");
            priceNode.put("Amount", inventory.getPrice());
            offerNode.set("Price", priceNode);
            offers.add(offerNode);
        }

        offerUpdateDTO.setOffers(offers);

        String json = objectMapper.writeValueAsString(offerUpdateDTO);

        return json;
    }
}
