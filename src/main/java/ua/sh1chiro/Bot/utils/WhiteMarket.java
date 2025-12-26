package ua.sh1chiro.Bot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import ua.sh1chiro.Bot.config.BotConfig;
import ua.sh1chiro.Bot.dto.EditBuyOrderResult;
import ua.sh1chiro.Bot.dto.InventoryDMarketDTO;
import ua.sh1chiro.Bot.dto.LowestSellInfo;
import ua.sh1chiro.Bot.dto.TargetPriceDTO;
import ua.sh1chiro.Bot.models.Offer;
import ua.sh1chiro.Bot.models.Target;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Created by Sh1chiro on 25.12.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public final class WhiteMarket {
    private static final String DEFAULT_ENDPOINT = "https://api.white.market/graphql/partner";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    private static final String partnerToken = BotConfig.config.getPublicAPIKey();
    @Getter
    private static volatile String accessToken;
    private static volatile Instant accessTokenIssuedAt;
    @Setter
    private static volatile String endpoint = DEFAULT_ENDPOINT;
    private static final Object AUTH_LOCK = new Object();

    private WhiteMarket() {}

    /**
     * Authorizes via partner token and caches access token (static).
     * Safe to call multiple times; it will just overwrite cached token.
     */
    public static String authorize() {
        final String pt = partnerToken;
        if (pt == null || pt.isBlank()) {
            throw new IllegalStateException("Partner token is empty. Set it via setPartnerToken(...) or env WHITE_MARKET_PARTNER_TOKEN.");
        }

        synchronized (AUTH_LOCK) {
            String query = "mutation{ auth_token{ accessToken } }";

            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("X-partner-token", pt);

            GraphQlCallResult res = postGraphQL(query, headers, null);

            JsonNode tokenNode = res.json.path("data").path("auth_token").path("accessToken");
            if (tokenNode.isMissingNode() || tokenNode.isNull() || tokenNode.asText().isBlank()) {
                throw new WhiteMarketException("auth_token returned empty accessToken. Full response: " + safeJson(res.json));
            }

            accessToken = tokenNode.asText();
            accessTokenIssuedAt = Instant.now();
            return accessToken;
        }
    }

    /**
     * Returns info about your own account (person_profile).
     * If gets 401 (either HTTP 401 OR GraphQL UNAUTHENTICATED), it will re-authorize and retry once.
     */
    public static PersonProfile getMe() {
        return getMe(false);
    }

    public static EditBuyOrderResult editBuyOrderPriceUsdCs2(
            String orderId,
            double newPriceUsd
    ) {
        return editBuyOrderPriceUsd("CSGO", orderId, newPriceUsd, false);
    }

    private static EditBuyOrderResult editBuyOrderPriceUsd(
            String appEnum,
            String orderId,
            double newPriceUsd,
            boolean alreadyRetried
    ) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        if (orderId == null || orderId.isBlank())
            throw new IllegalArgumentException("orderId is blank");
        if (newPriceUsd <= 0)
            throw new IllegalArgumentException("newPriceUsd must be > 0");

        String mutation =
                "mutation{" +
                        "order_edit(" +
                        "id:\"" + escapeGraphqlString(orderId) + "\" " +
                        "price:{ value:\"" + newPriceUsd + "\" currency:USD }" +
                        "quantity:1" +
                        "){" +
                        "id status price{ value currency }" +
                        "}" +
                        "}";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        // 401 → реавторизація
        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return editBuyOrderPriceUsd(appEnum, orderId, newPriceUsd, true);
            }
            throw new UnauthorizedException("Unauthorized (order_edit)");
        }

        // 4xx / 5xx
        if (res.httpStatus < 200 || res.httpStatus >= 300) {
            return new EditBuyOrderResult(
                    orderId,
                    null,
                    0,
                    "http_error",
                    "HTTP " + res.httpStatus + ": " + res.rawBody
            );
        }

        if (hasGraphQlErrors(res.json)) {
            return new EditBuyOrderResult(
                    orderId,
                    null,
                    0,
                    "graphql_error",
                    safeJson(res.json)
            );
        }

        JsonNode node = res.json.path("data").path("order_edit");
        if (node.isMissingNode() || node.isNull()) {
            return new EditBuyOrderResult(
                    orderId,
                    null,
                    0,
                    "empty_response",
                    "order_edit returned null"
            );
        }

        return new EditBuyOrderResult(
                textOrNull(node.get("id")),
                textOrNull(node.get("status")),
                safeParseDouble(node.path("price").path("value").asText()),
                null,
                null
        );
    }

    public static Optional<DealHistoryHit> findDealInHistoryByProductId(String productId, int limit) {
        return findDealInHistoryByProductId(productId, limit, false);
    }

    public static List<DealHistoryItem> getAllDealsHistoryCs2(
            String statusEnumOrNull,
            int pageSize,
            int maxPages
    ) {
        if (pageSize <= 0) pageSize = 50;
        if (maxPages <= 0) maxPages = 20;

        List<DealHistoryItem> all = new ArrayList<>();
        String cursor = null;

        for (int page = 0; page < maxPages; page++) {
            DealHistoryPage p = getDealsHistoryPageCs2(null, statusEnumOrNull, pageSize, cursor);
            if (p.items() != null) all.addAll(p.items());

            if (p.pageInfo() == null || !p.pageInfo().hasNextPage()) break;
            cursor = p.pageInfo().endCursor();
            if (cursor == null || cursor.isBlank()) break;
        }

        return all;
    }


    public static DealHistoryPage getDealsHistoryPageCs2(
            String nameHash,
            String statusEnumOrNull,
            int first,
            String afterCursor
    ) {
        return getDealsHistoryPage("CSGO", nameHash, statusEnumOrNull, first, afterCursor, false);
    }

    private static DealHistoryPage getDealsHistoryPage(
            String appEnum,
            String nameHash,
            String statusEnumOrNull,
            int first,
            String afterCursor,
            boolean alreadyRetried
    ) {
        if (accessToken == null || accessToken.isBlank()) authorize();
        if (first <= 0) first = 50;

        String afterPart = (afterCursor == null || afterCursor.isBlank())
                ? ""
                : "after:\"" + escapeGraphqlString(afterCursor) + "\"";

        StringBuilder search = new StringBuilder();
        // якщо хочеш звузити до конкретного item
        if (nameHash != null && !nameHash.isBlank()) {
            search.append("nameHash:\"").append(escapeGraphqlString(nameHash)).append("\" ");
            search.append("nameStrict:true ");
        }

        // статус (опціонально)
        if (statusEnumOrNull != null && !statusEnumOrNull.isBlank()) {
            // enum без лапок
            search.append("status: ").append(statusEnumOrNull).append(" ");
        }

        // сортування за датою (як у доках)
        search.append("sort:{ field: CREATED_AT type: DESC } ");

        String query =
                "query{ deal_history(" +
                        (search.length() > 0 ? "search:{ " + search + " } " : "") +
                        "forwardPagination:{ first:" + first + " " + afterPart + " }" +
                        "){ " +
                        "totalCount " +
                        "pageInfo{ hasNextPage endCursor } " +
                        "edges{ node{ " +
                        "id customId " +
                        // seller/buyer
                        "seller{ id steamId steamName } " +
                        "buyer{ id steamId steamName } " +
                        // product (історичний product) — нам потрібен product.id + ціна + опис
                        "product{ id appId contextId assetId status archivedAt " +
                        "price{ value currency } " +
                        "description{ nameHash name icon iconLarge } " +
                        "} " +
                        "} }" +
                        "} }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getDealsHistoryPage(appEnum, nameHash, statusEnumOrNull, first, afterCursor, true);
            }
            throw new UnauthorizedException("Unauthorized (deal_history) even after re-auth. Response: " + safeJson(res.json));
        } else if (res.httpStatus < 200 || res.httpStatus >= 300) {
            throw new WhiteMarketException("HTTP " + res.httpStatus + " body=" + res.rawBody);
        }

        assertNoGraphQlErrors(res.json);

        JsonNode root = res.json.path("data").path("deal_history");
        JsonNode edges = root.path("edges");

        List<DealHistoryItem> items = new ArrayList<>();
        if (edges.isArray()) {
            for (JsonNode e : edges) {
                JsonNode n = e.path("node");

                JsonNode seller = n.path("seller");
                JsonNode buyer = n.path("buyer");
                JsonNode product = n.path("product");
                JsonNode price = product.path("price");
                JsonNode desc = product.path("description");

                DealHistoryItem item = new DealHistoryItem(
                        textOrNull(n.get("id")),
                        textOrNull(n.get("customId")),
                        textOrNull(n.get("createdAt")), // якщо поля нема — буде null, ок
                        seller.isMissingNode() || seller.isNull() ? null : new PersonPublic(
                                textOrNull(seller.get("id")),
                                textOrNull(seller.get("steamId")),
                                textOrNull(seller.get("steamName"))
                        ),
                        buyer.isMissingNode() || buyer.isNull() ? null : new PersonPublic(
                                textOrNull(buyer.get("id")),
                                textOrNull(buyer.get("steamId")),
                                textOrNull(buyer.get("steamName"))
                        ),
                        product.isMissingNode() || product.isNull() ? null : new ProductHistory(
                                textOrNull(product.get("id")),
                                intOrNull(product.get("appId")),
                                intOrNull(product.get("contextId")),
                                textOrNull(product.get("assetId")),
                                (price.isMissingNode() || price.isNull()) ? null : new Money(
                                        textOrNull(price.get("value")),
                                        textOrNull(price.get("currency"))
                                ),
                                textOrNull(product.get("status")),
                                textOrNull(product.get("archivedAt")),
                                (desc.isMissingNode() || desc.isNull()) ? null : new SteamItemDesc(
                                        textOrNull(desc.get("nameHash")),
                                        textOrNull(desc.get("name")),
                                        textOrNull(desc.get("icon")),
                                        textOrNull(desc.get("iconLarge"))
                                )
                        )
                );

                items.add(item);
            }
        }

        JsonNode pi = root.path("pageInfo");
        PageInfo pageInfo = new PageInfo(
                pi.path("hasNextPage").asBoolean(false),
                textOrNull(pi.get("endCursor"))
        );

        return new DealHistoryPage(items, pageInfo, intOrNull(root.get("totalCount")));
    }


    private static Optional<DealHistoryHit> findDealInHistoryByProductId(String productId, int limit, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("productId is blank");
        if (limit <= 0) limit = 50;

        // Беремо просто останні "limit" записів історії.
        // За потреби можеш додати createdAt.from/to.
        String q =
                "query{ deal_history(" +
                        "search:{ sort:{ field: CREATED_AT type: DESC } } " +
                        "forwardPagination:{ first:" + limit + " }" +
                        "){ edges{ node{ " +
                        "id status createdAt " +
                        "product{ params{ product{ id slug price{ value currency } } } } " +
                        "} } } }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(q, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return findDealInHistoryByProductId(productId, limit, true);
            }
            throw new UnauthorizedException("Unauthorized (deal_history) even after re-auth. Response: " + safeJson(res.json));
        }
        assertNoGraphQlErrors(res.json);

        JsonNode edges = res.json.path("data").path("deal_history").path("edges");
        if (!edges.isArray()) return Optional.empty();

        for (JsonNode e : edges) {
            JsonNode n = e.path("node");

            JsonNode p = n.path("product").path("params").path("product");
            String pid = textOrNull(p.get("id"));
            if (pid == null) continue;

            if (pid.equals(productId)) {
                String slug = textOrNull(p.get("slug"));

                JsonNode price = p.path("price");
                double priceUsd = safeParseDouble(textOrNull(price.get("value")));

                return Optional.of(new DealHistoryHit(
                        textOrNull(n.get("id")),
                        textOrNull(n.get("status")),
                        pid,
                        slug,
                        priceUsd,
                        textOrNull(n.get("createdAt"))
                ));
            }
        }

        return Optional.empty();
    }

    public static java.util.List<EditOfferResult> editSellOffersPricesUsdCs2(java.util.List<Offer> offers) {
        return editSellOffersPricesUsd(offers, false);
    }

    private static java.util.List<EditOfferResult> editSellOffersPricesUsd(java.util.List<Offer> offers, boolean alreadyRetried) {
        if (offers == null || offers.isEmpty()) return java.util.List.of();
        if (accessToken == null || accessToken.isBlank()) authorize();

        // нормалізуємо список (щоб індекси були стабільні)
        java.util.List<Offer> normalized = new java.util.ArrayList<>();
        for (Offer o : offers) if (o != null) normalized.add(o);
        if (normalized.isEmpty()) return java.util.List.of();

        // 1) заздалегідь відмічаємо invalid і формуємо validItems + мапу sentIndex -> originalIndex
        java.util.Map<Integer, EditOfferResult> prefilled = new java.util.HashMap<>();
        java.util.List<Integer> sentToOriginal = new java.util.ArrayList<>();

        StringBuilder validItems = new StringBuilder();
        validItems.append("[");

        for (int originalIndex = 0; originalIndex < normalized.size(); originalIndex++) {
            Offer o = normalized.get(originalIndex);

            String productId = o.getProductId();
            double priceUsd = o.getPrice();

            if (productId == null || productId.isBlank() || priceUsd <= 0) {
                prefilled.put(originalIndex, new EditOfferResult(
                        originalIndex,
                        o.getId() == null ? null : String.valueOf(o.getId()),
                        null,
                        productId,
                        null,
                        null,
                        "USD",
                        "invalid_input",
                        "Skip: productId blank or priceUsd <= 0"
                ));
                continue;
            }

            if (!sentToOriginal.isEmpty()) validItems.append(", ");
            sentToOriginal.add(originalIndex);

            String pid = escapeGraphqlString(productId);
            String value = java.math.BigDecimal.valueOf(priceUsd)
                    .setScale(2, java.math.RoundingMode.HALF_UP)
                    .toPlainString();

            validItems.append("{ ")
                    .append("productId: \"").append(pid).append("\" ")
                    .append("price: { value: \"").append(value).append("\" currency: USD } ")
                    .append("}");
        }

        validItems.append("]");

        // якщо немає валідних — повертаємо тільки invalid
        if (sentToOriginal.isEmpty()) {
            java.util.List<EditOfferResult> onlyInvalid = new java.util.ArrayList<>();
            for (int i = 0; i < normalized.size(); i++) {
                onlyInvalid.add(prefilled.getOrDefault(i, new EditOfferResult(
                        i,
                        normalized.get(i).getId() == null ? null : String.valueOf(normalized.get(i).getId()),
                        null,
                        normalized.get(i).getProductId(),
                        null,
                        null,
                        "USD",
                        "invalid_input",
                        "No valid items"
                )));
            }
            return onlyInvalid;
        }

        String mutation =
                "mutation{ market_edit_multiple(items: " + validItems + "){ " +
                        "index " +
                        "product{ " +
                        "  __typename " +
                        "  ... on MarketProduct { id slug price{ value currency } } " +
                        "  ... on MarketProductHistory { id price{ value currency } } " +
                        "} " +
                        "errorCategory " +
                        "errorMessage " +
                        "} }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return editSellOffersPricesUsd(offers, true);
            }
            throw new UnauthorizedException("Unauthorized (market_edit_multiple) even after re-auth. Response: " + res.rawBody);
        }
        if (res.httpStatus < 200 || res.httpStatus >= 300) {
            throw new WhiteMarketException("HTTP " + res.httpStatus + " body=" + res.rawBody);
        }

        assertNoGraphQlErrors(res.json);

        JsonNode arr = res.json.path("data").path("market_edit_multiple");
        if (!arr.isArray()) {
            throw new WhiteMarketException("market_edit_multiple missing/invalid. Full: " + safeJson(res.json));
        }

        java.util.Map<Integer, EditOfferResult> edited = new java.util.HashMap<>();

        for (JsonNode n : arr) {
            int sentIndex = n.path("index").asInt(-1);
            if (sentIndex < 0 || sentIndex >= sentToOriginal.size()) continue;

            int originalIndex = sentToOriginal.get(sentIndex);
            Offer o = normalized.get(originalIndex);

            JsonNode product = n.path("product");
            String typeName = textOrNull(product.get("__typename"));

            String pid = null, slug = null, priceValue = null, priceCurrency = null;
            if (product != null && !product.isMissingNode() && !product.isNull()) {
                pid = textOrNull(product.get("id"));
                slug = textOrNull(product.get("slug"));
                JsonNode price = product.path("price");
                priceValue = textOrNull(price.get("value"));
                priceCurrency = textOrNull(price.get("currency"));
            }

            edited.put(originalIndex, new EditOfferResult(
                    originalIndex,
                    o.getId() == null ? null : String.valueOf(o.getId()),
                    typeName,
                    pid,
                    slug,
                    priceValue,
                    priceCurrency,
                    textOrNull(n.get("errorCategory")),
                    textOrNull(n.get("errorMessage"))
            ));
        }

        // фінальний список у порядку originalIndex
        java.util.List<EditOfferResult> finalOut = new java.util.ArrayList<>(normalized.size());
        for (int i = 0; i < normalized.size(); i++) {
            EditOfferResult r = edited.get(i);
            if (r != null) {
                finalOut.add(r);
                continue;
            }
            EditOfferResult pre = prefilled.get(i);
            if (pre != null) {
                finalOut.add(pre);
                continue;
            }

            Offer o = normalized.get(i);
            finalOut.add(new EditOfferResult(
                    i,
                    o.getId() == null ? null : String.valueOf(o.getId()),
                    null,
                    o.getProductId(),
                    null,
                    null,
                    "USD",
                    "unknown",
                    "No result for this item (missing index in response)"
            ));
        }

        return finalOut;
    }


    public static Order createBuyOrderCs2(String nameHash, BigDecimal priceUsd) {
        return createBuyOrder("CSGO", nameHash, priceUsd, "USD", 1, false);
    }

    public static ProductBrief editSellPriceUsd(String marketProductId, BigDecimal newPriceUsd) {
        return editSellPriceUsd(marketProductId, newPriceUsd, false);
    }

    private static ProductBrief editSellPriceUsd(String marketProductId, BigDecimal newPriceUsd, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        if (marketProductId == null || marketProductId.isBlank())
            throw new IllegalArgumentException("marketProductId is blank");
        if (newPriceUsd == null || newPriceUsd.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("newPriceUsd must be > 0");

        String id = escapeGraphqlString(marketProductId);
        String value = newPriceUsd.stripTrailingZeros().toPlainString();

        String mutation =
                "mutation{ market_edit(" +
                        "marketProductId:\"" + id + "\" " +
                        "price:{ value:\"" + value + "\" currency: USD }" +
                        "){ id slug price{ value currency } } }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return editSellPriceUsd(marketProductId, newPriceUsd, true);
            }
            throw new UnauthorizedException("Unauthorized (market_edit) even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        JsonNode p = res.json.path("data").path("market_edit");
        if (p.isMissingNode() || p.isNull()) {
            throw new WhiteMarketException("market_edit is missing in response. Full response: " + safeJson(res.json));
        }

        JsonNode price = p.path("price");
        return new ProductBrief(
                textOrNull(p.get("id")),
                textOrNull(p.get("slug")),
                null, null, null,
                price.isMissingNode() ? null : textOrNull(price.get("value")),
                price.isMissingNode() ? null : textOrNull(price.get("currency"))
        );
    }

    /**
     * Створює buy-ордер (таргет) по nameHash + ціні.
     * Якщо отримали 401 -> re-auth -> повторюємо 1 раз.
     *
     * appEnum: "CSGO" для CS2 (enum у GraphQL, без лапок)
     * currencyEnum: "USD" (enum у GraphQL, без лапок)
     */
    public static Order createBuyOrder(String appEnum, String nameHash, BigDecimal price, String currencyEnum, int quantity) {
        return createBuyOrder(appEnum, nameHash, price, currencyEnum, quantity, false);
    }

    private static Order createBuyOrder(String appEnum, String nameHash, BigDecimal price, String currencyEnum, int quantity, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) {
            authorize();
        }

        if (appEnum == null || appEnum.isBlank()) throw new IllegalArgumentException("appEnum is blank (e.g. CSGO)");
        if (currencyEnum == null || currencyEnum.isBlank()) throw new IllegalArgumentException("currencyEnum is blank (e.g. USD)");
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("price must be > 0");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");

        String nh = escapeGraphqlString(nameHash);
        String value = price.stripTrailingZeros().toPlainString();

        String mutation =
                "mutation{ order_new(" +
                        "app: " + appEnum + " " +
                        "nameHash: \"" + nh + "\" " +
                        "price: { value: \"" + value + "\" currency: " + currencyEnum + " } " +
                        "quantity: " + quantity +
                        "){ " +
                        "id app nameHash quantity status createdAt expiredAt " +
                        "price{ value currency } " +
                        "} }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        // 401 -> refresh + retry once
        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return createBuyOrder(appEnum, nameHash, price, currencyEnum, quantity, true);
            }
            throw new UnauthorizedException("Unauthorized while creating order even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        JsonNode o = res.json.path("data").path("order_new");
        if (o.isMissingNode() || o.isNull()) {
            throw new WhiteMarketException("order_new is missing in response. Full response: " + safeJson(res.json));
        }

        JsonNode money = o.path("price");
        return new Order(
                textOrNull(o.get("id")),
                textOrNull(o.get("app")),
                textOrNull(o.get("nameHash")),
                intOrNull(o.get("quantity")),
                textOrNull(o.get("status")),
                textOrNull(o.get("createdAt")),
                textOrNull(o.get("expiredAt")),
                money.isMissingNode() ? null : textOrNull(money.get("value")),
                money.isMissingNode() ? null : textOrNull(money.get("currency"))
        );
    }

    /**
     * (Опціонально) Пошук nameHash по назві (або частині назви).
     * Повертає список унікальних nameHash (distinctValues: true).
     *
     * Це корисно, якщо ти вводиш "Vulcan", а треба точний "AK-47 | Vulcan".
     */
    public static List<String> suggestNameHashesCs2(String namePart, int limit) {
        return suggestNameHashes("CSGO", namePart, limit, false);
    }

    private static List<String> suggestNameHashes(String appEnum, String namePart, int limit, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();
        if (limit <= 0) limit = 10;

        String q = escapeGraphqlString(namePart == null ? "" : namePart);

        String query =
                "query{ market_list(" +
                        "search:{ appId: " + appEnum + " name: \"" + q + "\" distinctValues: true nameStrict: false } " +
                        "forwardPagination:{ first: " + limit + " }" +
                        "){ edges{ node{ item{ description{ nameHash } } } } } }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return suggestNameHashes(appEnum, namePart, limit, true);
            }
            throw new UnauthorizedException("Unauthorized while market_list even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        List<String> out = new ArrayList<>();
        JsonNode edges = res.json.path("data").path("market_list").path("edges");
        if (edges.isArray()) {
            for (JsonNode e : edges) {
                String nh = e.path("node").path("item").path("description").path("nameHash").asText(null);
                if (nh != null && !nh.isBlank() && !out.contains(nh)) out.add(nh);
            }
        }
        return out;
    }

    private static String escapeGraphqlString(String s) {
        // мінімальний ескейп для GraphQL string
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static List<SellResult> sellOffersCs2(List<Offer> offers) {
        return sellOffersCs2(offers, false);
    }

    private static List<SellResult> sellOffersCs2(List<Offer> offers, boolean alreadyRetried) {
        if (offers == null || offers.isEmpty()) return List.of();
        if (accessToken == null || accessToken.isBlank()) authorize();

        List<Offer> idxToOffer = new ArrayList<>();
        List<String> gqlItems = new ArrayList<>();
        List<SellResult> out = new ArrayList<>();

        for (int i = 0; i < offers.size(); i++) {
            Offer o = offers.get(i);
            idxToOffer.add(o);

            if (o == null) {
                gqlItems.add(null);
                out.add(SellResult.fail(i, null, "local.validation", "Offer is null"));
                continue;
            }

            if (o.getInventoryId() == null || o.getInventoryId().isBlank()) {
                gqlItems.add(null);
                out.add(SellResult.fail(i, o, "local.validation", "inventoryId is blank"));
                continue;
            }

            if (o.getAssetId() == null || o.getAssetId().isBlank()) {
                gqlItems.add(null);
                out.add(SellResult.fail(i, o, "local.validation", "assetId is blank"));
                continue;
            }

            if (o.getPrice() <= 0) {
                gqlItems.add(null);
                out.add(SellResult.fail(i, o, "local.validation", "price must be > 0"));
                continue;
            }

            if (!o.isTradable()) {
                gqlItems.add(null);
                out.add(SellResult.fail(i, o, "local.validation", "Offer is not tradable"));
                continue;
            }

            String inventoryId = escapeGraphqlString(o.getInventoryId().trim());
            String assetId = escapeGraphqlString(o.getAssetId().trim());
            String priceValue = BigDecimal.valueOf(o.getPrice()).stripTrailingZeros().toPlainString();

            String item =
                    "{ " +
                            "inventoryId: \"" + inventoryId + "\" " +
                            "assetId: \"" + assetId + "\" " +
                            "price: { value: \"" + escapeGraphqlString(priceValue) + "\" currency: USD }" +
                            " }";

            System.out.println(item);

            gqlItems.add(item);
            out.add(null);
        }

        List<Integer> requestIndexToOfferIndex = new ArrayList<>();
        StringBuilder itemsArray = new StringBuilder("[");
        boolean first = true;

        for (int offerIndex = 0; offerIndex < gqlItems.size(); offerIndex++) {
            String item = gqlItems.get(offerIndex);
            if (item == null) continue;

            if (!first) itemsArray.append(",");
            itemsArray.append(item);
            first = false;

            requestIndexToOfferIndex.add(offerIndex);
        }
        itemsArray.append("]");

        // Якщо нема що виставляти — повернемо тільки локальні помилки
        if (requestIndexToOfferIndex.isEmpty()) {
            for (int i = 0; i < out.size(); i++) {
                if (out.get(i) == null) {
                    out.set(i, SellResult.fail(i, idxToOffer.get(i), "local.validation", "No valid items to sell"));
                }
            }
            return out;
        }

        // 3) GraphQL mutation market_new_multiple
        String mutation =
                "mutation{ market_new_multiple(" +
                        "items: " + itemsArray + " " +
                        "deliveryType: SEMI " +
                        "deliveryPromise: HOUR1 " +
                        "lifetime: 31536000" +
                        "){ " +
                        "index " +
                        "errorCategory " +
                        "errorMessage " +
                        "product{ __typename " +
                        "... on MarketProduct { id slug price{ value currency } } " +
                        "... on MarketProductHistory { id } " +
                        "} " +
                        "} }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        // 401 -> reauth -> retry once
        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return sellOffersCs2(offers, true);
            }
            throw new UnauthorizedException("Unauthorized (market_new_multiple) even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        JsonNode arr = res.json.path("data").path("market_new_multiple");
        if (!arr.isArray()) {
            throw new WhiteMarketException("market_new_multiple returned non-array. Full response: " + safeJson(res.json));
        }

        // 4) Розкладаємо відповіді по своїм Offer'ам
        for (JsonNode r : arr) {
            Integer idxRaw = intOrNull(r.get("index"));
            String errCat = textOrNull(r.get("errorCategory"));
            String errMsg = textOrNull(r.get("errorMessage"));

            int requestIdx = (idxRaw == null ? -1 : idxRaw);

            int offerIdx = -1;
            if (requestIdx >= 0 && requestIdx < requestIndexToOfferIndex.size()) {
                offerIdx = requestIndexToOfferIndex.get(requestIdx);
            } else if (requestIdx - 1 >= 0 && requestIdx - 1 < requestIndexToOfferIndex.size()) {
                // на всякий, якщо API раптом 1-based
                offerIdx = requestIndexToOfferIndex.get(requestIdx - 1);
            } else {
                continue;
            }

            Offer offer = idxToOffer.get(offerIdx);

            JsonNode product = r.get("product");
            String productId = null;
            String slug = null;
            if (product != null && !product.isNull() && !product.isMissingNode()) {
                productId = textOrNull(product.get("id"));
                slug = textOrNull(product.get("slug"));
            }

            SellResult sr = new SellResult(
                    offerIdx,
                    offer == null ? null : offer.getAssetId(),
                    offer == null ? null : offer.getInventoryId(),
                    productId,
                    slug,
                    errCat,
                    errMsg
            );

            // якщо успіх — запишемо offerId (productId)
            if (offer != null && productId != null && (errCat == null && errMsg == null)) {
                offer.setOfferId(productId);
            }

            out.set(offerIdx, sr);
        }

        // доб’ємо null-и (якщо сервер не повернув result для якогось item)
        for (int i = 0; i < out.size(); i++) {
            if (out.get(i) == null) {
                out.set(i, SellResult.fail(i, idxToOffer.get(i), "unknown", "No result returned for this item"));
            }
        }

        return out;
    }

    /** Результат виставлення на продаж одного Offer */
    public record SellResult(
            int offerIndex,
            String assetId,
            String inventoryId,
            String productId,
            String slug,
            String errorCategory,
            String errorMessage
    ){
        public boolean isOk() {
            return productId != null && (errorCategory == null && errorMessage == null);
        }

        public static SellResult fail(int idx, Offer o, String cat, String msg) {
            return new SellResult(
                    idx,
                    o == null ? null : o.getAssetId(),
                    o == null ? null : o.getInventoryId(),
                    null,
                    null,
                    cat,
                    msg
            );
        }
    }

    public static List<PublicOrder> getPublicBuyOrdersCs2ByNameHash(String nameHash, int limit) {
        return getPublicBuyOrdersByNameHash("CSGO", nameHash, limit, false);
    }

    public static List<TargetPriceDTO> getPublicBuyTargetsCs2(String nameHash, int limit) {
        var orders = getPublicBuyOrdersCs2ByNameHash(nameHash, limit);

        List<TargetPriceDTO> out = new ArrayList<>();
        for (PublicOrder o : orders) {
            if (o == null) continue;

            double price = safeParseDouble(o.priceValue());
            int qty = o.quantity() == null ? 0 : o.quantity();

            if (price <= 0 || qty <= 0) continue;

            out.add(new TargetPriceDTO(o.id(), price, qty));
        }

        out.sort(Comparator.comparingDouble(TargetPriceDTO::getPrice).reversed());
        return out;
    }

    private static double safeParseDouble(String s) {
        if (s == null || s.isBlank()) return 0d;
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0d;
        }
    }

    /**
     * Публічні ордери (всі на платформі) по nameHash.
     * Реально корисно, щоб подивитись “які bid-и стоять” по цьому скіну.
     *
     * Використовує query order_list(search: { nameHash, nameStrict }) :contentReference[oaicite:2]{index=2}
     */
    public static List<PublicOrder> getPublicBuyOrdersByNameHash(String appEnum, String nameHash, int limit) {
        return getPublicBuyOrdersByNameHash(appEnum, nameHash, limit, false);
    }

    private static List<PublicOrder> getPublicBuyOrdersByNameHash(String appEnum, String nameHash, int limit, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        if (appEnum == null || appEnum.isBlank()) throw new IllegalArgumentException("appEnum is blank (e.g. CSGO)");
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");
        if (limit <= 0) limit = 50;

        String nh = escapeGraphqlString(nameHash);

        String query =
                "query{" +
                        "order_list(" +
                        "search:{" +
                        "appId:" + appEnum + " " +
                        "nameHash:\"" + nh + "\" " +
                        "nameStrict:true " +
                        "distinctValues:false" +
                        "} " +
                        "forwardPagination:{first:" + limit + "}" +
                        "){" +
                        "edges{ node{ id app nameHash quantity price{ value currency } } }" +
                        "pageInfo{ hasNextPage endCursor } " +
                        "totalCount" +
                        "}" +
                        "}";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getPublicBuyOrdersByNameHash(appEnum, nameHash, limit, true);
            }
            throw new UnauthorizedException("Unauthorized (order_list) even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);
        System.out.println(res.json);
        List<PublicOrder> out = new ArrayList<>();
        JsonNode edges = res.json.path("data").path("order_list").path("edges");
        if (edges.isArray()) {
            for (JsonNode e : edges) {
                JsonNode n = e.path("node");
                JsonNode price = n.path("price");
                out.add(new PublicOrder(
                        textOrNull(n.get("id")),
                        textOrNull(n.get("app")),
                        textOrNull(n.get("nameHash")),
                        intOrNull(n.get("quantity")),
                        textOrNull(price.get("value")),
                        textOrNull(price.get("currency"))
                ));
            }
        }
        return out;
    }

    public static List<BuyTargetResult> createBuyTargetsCs2Batch(List<Target> targets, int batchSize) {
        if (targets == null || targets.isEmpty()) return List.of();
        if (batchSize <= 0) batchSize = 5;

        List<BuyTargetResult> all = new ArrayList<>();

        for (int from = 0; from < targets.size(); from += batchSize) {
            int to = Math.min(from + batchSize, targets.size());
            List<Target> chunk = targets.subList(from, to);

            List<BuyTargetResult> chunkRes = createBuyTargetsChunkWithRetry("CSGO", chunk, from);
            all.addAll(chunkRes);
        }

        return all;
    }

    public static Order createBuyTargetCs2(String nameHash, double priceUsd) {
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");
        if (priceUsd <= 0) throw new IllegalArgumentException("priceUsd must be > 0");

        return createBuyTarget("CSGO", nameHash, priceUsd, false);
    }

    private static Order createBuyTarget(String appEnum, String nameHash, double priceUsd, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        String nh = escapeGraphqlString(nameHash);
        String value = java.math.BigDecimal.valueOf(priceUsd).stripTrailingZeros().toPlainString();

        String mutation =
                "mutation{ order_new(" +
                        "app: " + appEnum + " " +
                        "nameHash: \"" + nh + "\" " +
                        "price: { value: \"" + value + "\" currency: USD } " +
                        "quantity: 1" +
                        "){ " +
                        "id app nameHash quantity status createdAt expiredAt " +
                        "price{ value currency } " +
                        "} }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        // 1) HTTP-level
        if (res.httpStatus == 401) {
            if (!alreadyRetried) {
                authorize();
                return createBuyTarget(appEnum, nameHash, priceUsd, true);
            }
            throw new UnauthorizedException("Unauthorized (HTTP 401) even after re-auth. Body: " + res.rawBody);
        }
        if (res.httpStatus < 200 || res.httpStatus >= 300) {
            throw new WhiteMarketException("HTTP " + res.httpStatus + " body=" + res.rawBody);
        }

        // 2) GraphQL-level UNAUTHENTICATED
        if (isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return createBuyTarget(appEnum, nameHash, priceUsd, true);
            }
            throw new UnauthorizedException("Unauthorized (GraphQL UNAUTHENTICATED) even after re-auth. Response: " + safeJson(res.json));
        }

        // 3) any GraphQL errors
        assertNoGraphQlErrors(res.json);

        JsonNode o = res.json.path("data").path("order_new");
        if (o.isMissingNode() || o.isNull()) {
            throw new WhiteMarketException("order_new is missing in response. Full: " + safeJson(res.json));
        }

        JsonNode money = o.path("price");
        return new Order(
                textOrNull(o.get("id")),
                textOrNull(o.get("app")),
                textOrNull(o.get("nameHash")),
                intOrNull(o.get("quantity")),
                textOrNull(o.get("status")),
                textOrNull(o.get("createdAt")),
                textOrNull(o.get("expiredAt")),
                money.isMissingNode() ? null : textOrNull(money.get("value")),
                money.isMissingNode() ? null : textOrNull(money.get("currency"))
        );
    }

    private static List<BuyTargetResult> createBuyTargetsChunkWithRetry(String appEnum, List<Target> chunk, int baseIndex) {
        int attempts = 0;
        long backoffMs = 900;

        while (true) {
            attempts++;

            if (accessToken == null || accessToken.isBlank()) authorize();

            GraphQlCallResult res = postGraphQL(buildBuyOrdersMutation(appEnum, chunk), authHeaders(), null);

            // 401 / UNAUTHENTICATED -> reauth once
            if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
                if (attempts >= 2) {
                    // повертаємо помилку на кожен елемент чанку
                    return buildSameErrorResults(chunk, baseIndex, "unauthorized", "401 / UNAUTHENTICATED");
                }
                authorize();
                continue;
            }

            // парсимо норм
            List<BuyTargetResult> parsed = parseBuyBatchResponse(res.json, chunk, baseIndex);

            // якщо є too_many_requests хоча б в одному — ретраїмо chunk (обмежено)
            boolean hasTooMany = parsed.stream().anyMatch(r ->
                    r.errorCategory() != null && (
                            r.errorCategory().equalsIgnoreCase("person.action_too_many_requests")
                                    || r.errorCategory().toLowerCase().contains("too_many")
                    )
            );

            if (hasTooMany && attempts < 4) {
                sleepSilently(backoffMs);
                backoffMs = Math.min(backoffMs * 2, 6000);
                continue;
            }

            return parsed;
        }
    }

    private static String buildBuyOrdersMutation(String appEnum, List<Target> chunk) {
        StringBuilder sb = new StringBuilder();
        sb.append("mutation{\n");

        for (int i = 0; i < chunk.size(); i++) {
            Target t = chunk.get(i);
            if (t == null) continue;

            String nameHash = t.getName();
            double priceUsd = t.getPrice();

            // навіть якщо назва/ціна криві — краще пропустити, ніж зробити 400 на весь батч
            if (nameHash == null || nameHash.isBlank() || priceUsd <= 0) continue;

            String alias = "o" + i;
            String nh = escapeGraphqlString(nameHash);
            String value = java.math.BigDecimal.valueOf(priceUsd).stripTrailingZeros().toPlainString();

            sb.append("  ")
                    .append(alias)
                    .append(": order_new(")                 // <-- ПРОБІЛ після ':'
                    .append("app: ").append(appEnum).append(" ")
                    .append("nameHash: \"").append(nh).append("\" ")
                    .append("price: { value: \"").append(value).append("\" currency: USD } ")
                    .append("quantity: 1")
                    .append(") { ")
                    .append("id status nameHash quantity ")
                    .append("price { value currency } ")
                    .append("icon iconLarge ")
                    .append("}\n");                         // <-- ПЕРЕНОС після кожного блоку
        }

        sb.append("}");

        return sb.toString();
    }

    private static List<BuyTargetResult> parseBuyBatchResponse(JsonNode root, List<Target> chunk, int baseIndex) {
        // 1) Зберемо помилки по alias: "o0" -> (category, message)
        Map<String, ErrorInfo> errorsByAlias = new HashMap<>();

        JsonNode errors = root.get("errors");
        if (errors != null && errors.isArray()) {
            for (JsonNode err : errors) {
                String msg = textOrNull(err.get("message"));

                String cat = null;
                JsonNode ext = err.get("extensions");
                if (ext != null && ext.isObject()) {
                    // у них буває category або code
                    cat = textOrNull(ext.get("category"));
                    if (cat == null) cat = textOrNull(ext.get("code"));
                }

                // path: ["o1", ...]
                String alias = null;
                JsonNode path = err.get("path");
                if (path != null && path.isArray() && path.size() > 0) {
                    alias = path.get(0).asText(null);
                }

                if (alias != null) {
                    errorsByAlias.put(alias, new ErrorInfo(cat, msg));
                } else {
                    // якщо без path — загальна помилка
                    errorsByAlias.put("_global", new ErrorInfo(cat, msg));
                }
            }
        }

        JsonNode data = root.get("data");

        List<BuyTargetResult> out = new ArrayList<>(chunk.size());

        for (int i = 0; i < chunk.size(); i++) {
            Target t = chunk.get(i);
            String nameHash = (t == null) ? null : t.getName();
            double priceUsd = (t == null) ? 0d : t.getPrice();

            String alias = "o" + i;

            // Якщо твої дані невалідні — віддаємо помилку без звернення до API логіки
            if (nameHash == null || nameHash.isBlank() || priceUsd <= 0) {
                out.add(new BuyTargetResult(baseIndex + i, nameHash, priceUsd, null, null, null, null,
                        "validation", "nameHash is blank OR price<=0"));
                continue;
            }

            // Якщо є помилка на цей alias — повертаємо її
            ErrorInfo ei = errorsByAlias.get(alias);
            if (ei == null) ei = errorsByAlias.get("_global"); // fallback

            JsonNode node = (data == null) ? null : data.get(alias);

            if (node == null || node.isNull()) {
                if (ei != null) {
                    out.add(new BuyTargetResult(baseIndex + i, nameHash, priceUsd, null, null, null, null,
                            ei.category(), ei.message()));
                } else {
                    out.add(new BuyTargetResult(baseIndex + i, nameHash, priceUsd, null, null, null, null,
                            "unknown", "No data for alias " + alias));
                }
                continue;
            }

            // success parse
            String id = textOrNull(node.get("id"));
            String status = textOrNull(node.get("status"));
            String icon = textOrNull(node.get("icon"));
            String iconLarge = textOrNull(node.get("iconLarge"));

            out.add(new BuyTargetResult(baseIndex + i, nameHash, priceUsd, id, status, icon, iconLarge, null, null));
        }

        return out;
    }

    private record ErrorInfo(String category, String message) {}


    private static Map<String, String> authHeaders() {
        var h = new LinkedHashMap<String, String>();
        h.put("Authorization", "Bearer " + accessToken);
        return h;
    }

    private static List<BuyTargetResult> buildSameErrorResults(List<Target> chunk, int baseIndex, String cat, String msg) {
        List<BuyTargetResult> out = new ArrayList<>(chunk.size());
        for (int i = 0; i < chunk.size(); i++) {
            Target t = chunk.get(i);
            String nh = t == null ? null : t.getName();
            double p = t == null ? 0d : t.getPrice();
            out.add(new BuyTargetResult(baseIndex + i, nh, p, null, null, null, null, cat, msg));
        }
        return out;
    }


    public static InventoryPage getInventoryCs2(int first, String afterCursor) {
        // Без фільтрів — просто перша сторінка
        return getInventory("CSGO", null, null, null, first, afterCursor, false);
    }

    /**
     * name — пошук по назві ("Vulcan"), nameStrict — строгий чи ні
     * nameHash — точний hash ("AK-47 | Vulcan") :contentReference[oaicite:3]{index=3}
     */
    public static InventoryPage getInventoryByName(String name, boolean nameStrict, int first, String afterCursor) {
        return getInventory("CSGO", name, nameStrict, null, first, afterCursor, false);
    }

    public static LowestSell getLowestSellPriceWithIdUsdCs2(String nameHash) {
        return getLowestSellPriceWithIdUsd("CSGO", nameHash, false);
    }

    private static LowestSell getLowestSellPriceWithIdUsd(String appEnum, String nameHash, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        if (appEnum == null || appEnum.isBlank()) throw new IllegalArgumentException("appEnum is blank (e.g. CSGO)");
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");

        String nh = escapeGraphqlString(nameHash);

        // беремо найнижчу ціну + пробуємо витягнути id лістингу/продукту
        String query =
                "query{ market_list(" +
                        "search:{ " +
                        "appId:" + appEnum + " " +
                        "nameHash:\"" + nh + "\" " +
                        "nameStrict:true " +
                        "distinctValues:false " +
                        "sort:{ field: PRICE type: ASC } " +
                        "} " +
                        "forwardPagination:{ first:1 }" +
                        "){ edges{ node{ " +
                        "id " +                              // часто це id лістингу
                        "price{ value currency } " +
                        "item{ product{ id } } " +           // інколи productId тут
                        "} } } }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        // 401 -> refresh + retry once
        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getLowestSellPriceWithIdUsd(appEnum, nameHash, true);
            }
            throw new UnauthorizedException("Unauthorized (market_list) even after re-auth. Response: " + safeJson(res.json));
        }

        if (res.httpStatus < 200 || res.httpStatus >= 300) {
            throw new WhiteMarketException("HTTP " + res.httpStatus + " body=" + res.rawBody);
        }

        assertNoGraphQlErrors(res.json);

        JsonNode edges = res.json.path("data").path("market_list").path("edges");
        if (!edges.isArray() || edges.size() == 0) {
            return new LowestSell(0.0d, null);
        }

        JsonNode node = edges.get(0).path("node");

        // id лістингу або productId
        String id = textOrNull(node.get("id"));
        if (id == null) {
            id = textOrNull(node.path("item").path("product").get("id"));
        }

        String value = textOrNull(node.path("price").get("value"));
        if (value == null || value.isBlank()) {
            return new LowestSell(0.0d, id);
        }

        double price;
        try {
            price = Double.parseDouble(value);
        } catch (Exception e) {
            price = 0.0d;
        }

        return new LowestSell(price, id);
    }

    public static double getLowestSellPriceUsdCs2(String nameHash) {
        return getLowestSellPriceUsd("CSGO", nameHash, false);
    }

    private static double getLowestSellPriceUsd(String appEnum, String nameHash, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        if (appEnum == null || appEnum.isBlank()) throw new IllegalArgumentException("appEnum is blank (e.g. CSGO)");
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");

        String nh = escapeGraphqlString(nameHash);

        String query =
                "query{ market_list(" +
                        "search:{ " +
                        "appId:" + appEnum + " " +
                        "nameHash:\"" + nh + "\" " +
                        "nameStrict:true " +
                        "distinctValues:false " +
                        "sort:{ field: PRICE type: ASC } " +
                        "} " +
                        "forwardPagination:{ first:1 }" +
                        "){ edges{ node{ price{ value currency } } } } }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        // 401 -> refresh + retry once
        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getLowestSellPriceUsd(appEnum, nameHash, true);
            }
            throw new UnauthorizedException("Unauthorized (market_list) even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        JsonNode edges = res.json.path("data").path("market_list").path("edges");
        if (!edges.isArray() || edges.size() == 0) {
            return 0.0d; // немає лістингів
        }

        JsonNode price = edges.get(0).path("node").path("price");
        String value = textOrNull(price.get("value"));
        if (value == null || value.isBlank()) return 0.0d;

        try {
            // Без BigDecimal — напряму в double
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0d; // якщо раптом прийшло щось дивне
        }
    }

    public static LowestSellInfo getLowestSellInfoUsdCs2(String nameHash) {
        return getLowestSellInfoUsd("CSGO", nameHash, false);
    }

    private static LowestSellInfo getLowestSellInfoUsd(String appEnum, String nameHash, boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();
        if (appEnum == null || appEnum.isBlank()) throw new IllegalArgumentException("appEnum is blank (e.g. CSGO)");
        if (nameHash == null || nameHash.isBlank()) throw new IllegalArgumentException("nameHash is blank");

        String nh = escapeGraphqlString(nameHash);

        // Варіант #1: node.item.description.iconLarge/icon
        String query1 =
                "query{ market_list(" +
                        "search:{ " +
                        "appId:" + appEnum + " " +
                        "nameHash:\"" + nh + "\" " +
                        "nameStrict:true " +
                        "distinctValues:false " +
                        "sort:{ field: PRICE type: ASC } " +
                        "} " +
                        "forwardPagination:{ first:1 }" +
                        "){ edges{ node{ " +
                        "price{ value currency } " +
                        "item{ description{ icon iconLarge nameHash name } } " +
                        "} } } }";

        // Варіант #2: node.description.iconLarge/icon (якщо item нема в схемі)
        String query2 =
                "query{ market_list(" +
                        "search:{ " +
                        "appId:" + appEnum + " " +
                        "nameHash:\"" + nh + "\" " +
                        "nameStrict:true " +
                        "distinctValues:false " +
                        "sort:{ field: PRICE type: ASC } " +
                        "} " +
                        "forwardPagination:{ first:1 }" +
                        "){ edges{ node{ " +
                        "price{ value currency } " +
                        "description{ icon iconLarge nameHash name } " +
                        "} } } }";

        var headers = new LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res;

        // пробуємо query1, якщо схема не підтримує — пробуємо query2
        try {
            res = postGraphQL(query1, headers, null);
            if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
                if (!alreadyRetried) {
                    authorize();
                    return getLowestSellInfoUsd(appEnum, nameHash, true);
                }
                throw new UnauthorizedException("Unauthorized (market_list) even after re-auth. Response: " + safeJson(res.json));
            }
            assertNoGraphQlErrors(res.json);
        } catch (WhiteMarketException ex) {
            // якщо GraphQL сказав, що поля нема — пробуємо альтернативу
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.contains("Cannot query field") || msg.contains("GraphQL errors")) {
                res = postGraphQL(query2, headers, null);

                if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
                    if (!alreadyRetried) {
                        authorize();
                        return getLowestSellInfoUsd(appEnum, nameHash, true);
                    }
                    throw new UnauthorizedException("Unauthorized (market_list) even after re-auth. Response: " + safeJson(res.json));
                }
                assertNoGraphQlErrors(res.json);
            } else {
                throw ex;
            }
        }

        JsonNode edges = res.json.path("data").path("market_list").path("edges");
        if (!edges.isArray() || edges.size() == 0) {
            return new LowestSellInfo(0.0d, null);
        }

        JsonNode node = edges.get(0).path("node");
        JsonNode priceNode = node.path("price");
        String value = textOrNull(priceNode.get("value"));

        double price = 0.0d;
        if (value != null && !value.isBlank()) {
            try { price = Double.parseDouble(value); } catch (Exception ignore) {}
        }

        // Витягуємо картинку: спочатку item.description, потім description
        String img = null;

        JsonNode d1 = node.path("item").path("description");
        img = textOrNull(d1.get("iconLarge"));
        if (img == null) img = textOrNull(d1.get("icon"));

        if (img == null) {
            JsonNode d2 = node.path("description");
            img = textOrNull(d2.get("iconLarge"));
            if (img == null) img = textOrNull(d2.get("icon"));
        }

        return new LowestSellInfo(price, img);
    }

    public static InventoryPage getInventoryByNameHash(String nameHash, int first, String afterCursor) {
        return getInventory("CSGO", null, null, nameHash, first, afterCursor, false);
    }

    private static InventoryPage getInventory(
            String appEnum,
            String name,
            Boolean nameStrict,
            String nameHash,
            int first,
            String afterCursor,
            boolean alreadyRetried
    ) {
        if (accessToken == null || accessToken.isBlank()) authorize();
        if (first <= 0) first = 50;

        StringBuilder search = new StringBuilder();
        boolean hasSearch = false;

        if (name != null && !name.isBlank()) {
            search.append("name:\"").append(escapeGraphqlString(name)).append("\" ");
            hasSearch = true;

            if (nameStrict != null) {
                search.append("nameStrict:").append(nameStrict).append(" ");
            }
        }

        if (nameHash != null && !nameHash.isBlank()) {
            search.append("nameHash:\"").append(escapeGraphqlString(nameHash)).append("\" ");
            // якщо передали nameHash — логічно строгий пошук
            search.append("nameStrict:true ");
            hasSearch = true;
        }

        // Сортування (можеш прибрати/змінити) :contentReference[oaicite:4]{index=4}
        search.append("sort:{ field: CREATED type: DESC } ");
        hasSearch = true;

        String afterPart = (afterCursor == null || afterCursor.isBlank())
                ? ""
                : "after:\"" + escapeGraphqlString(afterCursor) + "\"";

        String query =
                "query{ inventory_my(" +
                        (hasSearch ? "search:{ " + search + " } " : "") +
                        "forwardPagination:{ first:" + first + " " + afterPart + " }" +
                        "){ " +
                        "totalCount " +
                        "pageInfo{ hasNextPage endCursor } " +
                        "edges{ node{ " +
                        "id appId contextId assetId classId instanceId amount updatedAt " +
                        // description — SteamItemInterface (nameHash, icon, tradeable, etc.) :contentReference[oaicite:5]{index=5}
                        "description{ nameHash name icon iconLarge isTradeable marketTradeableRestriction updatedAt } " +
                        // product — якщо item вже виставлений/прив’язаний до MarketProduct (ціна/slug/...) :contentReference[oaicite:6]{index=6}
                        "product{ id slug deliveryType createdAt expiredAt price{ value currency } } " +
                        "} }" +
                        "} }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getInventory(appEnum, name, nameStrict, nameHash, first, afterCursor, true);
            }
            throw new UnauthorizedException("Unauthorized (inventory_my) even after re-auth. Response: " + safeJson(res.json));
        }

        assertNoGraphQlErrors(res.json);

        JsonNode root = res.json.path("data").path("inventory_my");
        JsonNode edges = root.path("edges");

        java.util.List<InventoryItem> items = new java.util.ArrayList<>();
        if (edges.isArray()) {
            for (JsonNode e : edges) {
                JsonNode n = e.path("node");
                JsonNode d = n.path("description");
                JsonNode p = n.path("product");
                JsonNode price = p.path("price");

                items.add(new InventoryItem(
                        textOrNull(n.get("id")),
                        intOrNull(n.get("appId")),
                        intOrNull(n.get("contextId")),
                        textOrNull(n.get("assetId")),
                        textOrNull(n.get("classId")),
                        textOrNull(n.get("instanceId")),
                        intOrNull(n.get("amount")),
                        textOrNull(n.get("updatedAt")),

                        new ItemDescription(
                                textOrNull(d.get("nameHash")),
                                textOrNull(d.get("name")),
                                textOrNull(d.get("icon")),
                                textOrNull(d.get("iconLarge")),
                                boolOrNull(d.get("isTradeable")),
                                intOrNull(d.get("marketTradeableRestriction")),
                                textOrNull(d.get("updatedAt"))
                        ),

                        p.isMissingNode() || p.isNull() ? null : new ProductBrief(
                                textOrNull(p.get("id")),
                                textOrNull(p.get("slug")),
                                textOrNull(p.get("deliveryType")),
                                textOrNull(p.get("createdAt")),
                                textOrNull(p.get("expiredAt")),
                                price.isMissingNode() ? null : textOrNull(price.get("value")),
                                price.isMissingNode() ? null : textOrNull(price.get("currency"))
                        )
                ));
            }
        }

        JsonNode pageInfo = root.path("pageInfo");
        PageInfo pi = new PageInfo(
                boolOrNull(pageInfo.get("hasNextPage")) != null && pageInfo.get("hasNextPage").asBoolean(),
                textOrNull(pageInfo.get("endCursor"))
        );

        return new InventoryPage(items, pi, intOrNull(root.get("totalCount")));
    }

    // DTOs
    public record InventoryPage(java.util.List<InventoryItem> items, PageInfo pageInfo, Integer totalCount) {}
    public record PageInfo(boolean hasNextPage, String endCursor) {}

    public static PersonProfile refreshInventoryFromSteam() {
        return refreshInventoryFromSteam(false);
    }

    public static java.util.List<InventoryDMarketDTO> getAllTradeableInventoryCs2(int pageSize) {
        var all = getAllInventoryCs2(pageSize);

        return all.stream()
                .filter(i -> i.description() != null && Boolean.TRUE.equals(i.description().isTradeable()))
                .filter(i -> i.product() == null)
                .map(i -> {
                    InventoryDMarketDTO dto = new InventoryDMarketDTO();

                    dto.setInventoryId(i.id());
                    dto.setAssetId(i.assetId());

                    String name = i.description().nameHash();
                    if (name == null || name.isBlank()) {
                        name = i.description().name();
                    }
                    dto.setName(name);

                    String img = i.description().iconLarge();
                    if (img == null || img.isBlank()) {
                        img = i.description().icon();
                    }
                    dto.setImageLink(img);

                    dto.setTradable(true);

                    return dto;
                })
                .toList();
    }

    private static PersonProfile refreshInventoryFromSteam(boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) authorize();

        String mutation =
                "mutation{ inventory_update{ " +
                        "id email steamId steamName steamTradeUrl registeredAt inventoryUpdatedAt steamLevel referrerCode referralsCount isEmailConfirmed" +
                        " } }";

        var headers = new java.util.LinkedHashMap<String, String>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(mutation, headers, null);

        if (res.httpStatus == 401 || isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return refreshInventoryFromSteam(true);
            }
            throw new UnauthorizedException("Unauthorized (inventory_update) even after re-auth. Response: " + safeJson(res.json));
        }else if (res.httpStatus < 200 || res.httpStatus >= 300) {
            throw new WhiteMarketException("HTTP " + res.httpStatus + " body=" + res.rawBody);
        }

        assertNoGraphQlErrors(res.json);

        JsonNode p = res.json.path("data").path("inventory_update");
        if (p.isMissingNode() || p.isNull()) {
            throw new WhiteMarketException("inventory_update is missing in response. Full response: " + safeJson(res.json));
        }

        return new PersonProfile(
                textOrNull(p.get("id")),
                textOrNull(p.get("email")),
                textOrNull(p.get("steamId")),
                textOrNull(p.get("steamName")),
                textOrNull(p.get("steamTradeUrl")),
                textOrNull(p.get("registeredAt")),
                textOrNull(p.get("inventoryUpdatedAt")),
                intOrNull(p.get("steamLevel")),
                textOrNull(p.get("referrerCode")),
                intOrNull(p.get("referralsCount")),
                boolOrNull(p.get("isEmailConfirmed"))
        );
    }

    public static java.util.List<InventoryItem> getAllInventoryCs2(int pageSize) {
        java.util.List<InventoryItem> all = new java.util.ArrayList<>();
        String cursor = null;

        while (true) {
            InventoryPage page = getInventoryCs2(pageSize, cursor);
            all.addAll(page.items());

            if (page.pageInfo() == null || !page.pageInfo().hasNextPage()) break;
            cursor = page.pageInfo().endCursor();
            if (cursor == null || cursor.isBlank()) break;
        }
        return all;
    }

    public record InventoryItem(
            String id,
            Integer appId,
            Integer contextId,
            String assetId,
            String classId,
            String instanceId,
            Integer amount,
            String updatedAt,
            ItemDescription description,
            ProductBrief product
    ) {}

    public record ItemDescription(
            String nameHash,
            String name,
            String icon,
            String iconLarge,
            Boolean isTradeable,
            Integer marketTradeableRestriction,
            String updatedAt
    ) {}

    public record ProductBrief(
            String id,
            String slug,
            String deliveryType,
            String createdAt,
            String expiredAt,
            String priceValue,
            String priceCurrency
    ) {}


    public record PublicOrder(
            String id,
            String app,
            String nameHash,
            Integer quantity,
            String priceValue,
            String priceCurrency
    ) {}

    // DTO
    public record Order(
            String id,
            String app,
            String nameHash,
            Integer quantity,
            String status,
            String createdAt,
            String expiredAt,
            String priceValue,
            String priceCurrency
    ) {}

    // ============================================================
    // INTERNAL
    // ============================================================

    private static PersonProfile getMe(boolean alreadyRetried) {
        if (accessToken == null || accessToken.isBlank()) {
            authorize();
        }

        String query =
                "query{ person_profile{ " +
                        "id email steamId steamName steamTradeUrl " +
                        "registeredAt inventoryUpdatedAt steamLevel referrerCode referralsCount isEmailConfirmed" +
                        " } }";

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer " + accessToken);

        GraphQlCallResult res = postGraphQL(query, headers, null);

        // If HTTP 401 -> refresh + retry once
        if (res.httpStatus == 401) {
            if (!alreadyRetried) {
                authorize();
                return getMe(true);
            }
            throw new UnauthorizedException("Unauthorized (HTTP 401) even after re-auth. Response: " + safeJson(res.json));
        }

        // Sometimes GraphQL returns 200 with errors "UNAUTHENTICATED"
        if (isUnauthenticatedGraphQl(res.json)) {
            if (!alreadyRetried) {
                authorize();
                return getMe(true);
            }
            throw new UnauthorizedException("Unauthorized (GraphQL UNAUTHENTICATED) even after re-auth. Response: " + safeJson(res.json));
        }

        // Any GraphQL errors -> throw
        assertNoGraphQlErrors(res.json);

        JsonNode p = res.json.path("data").path("person_profile");
        if (p.isMissingNode() || p.isNull()) {
            throw new WhiteMarketException("person_profile is missing in response. Full response: " + safeJson(res.json));
        }

        return new PersonProfile(
                textOrNull(p.get("id")),
                textOrNull(p.get("email")),
                textOrNull(p.get("steamId")),
                textOrNull(p.get("steamName")),
                textOrNull(p.get("steamTradeUrl")),
                textOrNull(p.get("registeredAt")),
                textOrNull(p.get("inventoryUpdatedAt")),
                intOrNull(p.get("steamLevel")),
                textOrNull(p.get("referrerCode")),
                intOrNull(p.get("referralsCount")),
                boolOrNull(p.get("isEmailConfirmed"))
        );
    }

    private static GraphQlCallResult postGraphQL(String query, Map<String, String> extraHeaders, String operationName) {
        Objects.requireNonNull(query, "query");

        String bodyJson;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("query", query);
            if (operationName != null && !operationName.isBlank()) payload.put("operationName", operationName);
            bodyJson = MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new WhiteMarketException("Failed to serialize GraphQL payload", e);
        }

        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson));

        if (extraHeaders != null) {
            for (Map.Entry<String, String> h : extraHeaders.entrySet()) {
                if (h.getValue() != null && !h.getValue().isBlank()) {
                    b.header(h.getKey(), h.getValue());
                }
            }
        }

        HttpResponse<String> resp;
        try {
            resp = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WhiteMarketException("HTTP request failed", e);
        }
        System.out.println(resp.body());
        System.out.println();

        JsonNode json = tryParseJson(resp.body());
        return new GraphQlCallResult(resp.statusCode(), resp.body(), json);
    }

    private static JsonNode tryParseJson(String s) {
        if (s == null || s.isBlank()) return MAPPER.createObjectNode();
        try {
            return MAPPER.readTree(s);
        } catch (Exception ignore) {
            return MAPPER.createObjectNode().put("raw", s);
        }
    }

    private static void assertNoGraphQlErrors(JsonNode root) {
        JsonNode errors = root.get("errors");
        if (errors != null && errors.isArray() && errors.size() > 0) {
            throw new WhiteMarketException("GraphQL errors: " + safeJson(errors));
        }
    }

    private static boolean isUnauthenticatedGraphQl(JsonNode root) {
        JsonNode errors = root.get("errors");
        if (errors == null || !errors.isArray()) return false;

        for (JsonNode err : errors) {
            String msg = textOrNull(err.get("message"));
            if (msg != null && msg.toLowerCase().contains("unauth")) return true;

            JsonNode ext = err.get("extensions");
            if (ext != null) {
                String code = textOrNull(ext.get("code"));
                if (code != null && code.equalsIgnoreCase("UNAUTHENTICATED")) return true;

                // Some APIs put http status inside extensions
                JsonNode status = ext.get("status");
                if (status != null && status.isInt() && status.asInt() == 401) return true;
            }
        }
        return false;
    }

    private static String safeJson(JsonNode node) {
        try {
            return MAPPER.writeValueAsString(node);
        } catch (Exception e) {
            return String.valueOf(node);
        }
    }

    private static String textOrNull(JsonNode n) {
        if (n == null || n.isNull()) return null;
        String s = n.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static Integer intOrNull(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isInt()) return n.asInt();
        String s = n.asText();
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (Exception e) { return null; }
    }

    private static Boolean boolOrNull(JsonNode n) {
        if (n == null || n.isNull()) return null;
        if (n.isBoolean()) return n.asBoolean();
        String s = n.asText();
        if (s == null || s.isBlank()) return null;
        return Boolean.parseBoolean(s);
    }

    public record BuyTargetResult(
            int targetIndex,
            String nameHash,
            double priceUsd,
            String orderId,
            String status,
            String icon,
            String iconLarge,
            String errorCategory,
            String errorMessage
    ) {}
    private static void sleepSilently(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public record PersonProfile(
            String id,
            String email,
            String steamId,
            String steamName,
            String steamTradeUrl,
            String registeredAt,
            String inventoryUpdatedAt,
            Integer steamLevel,
            String referrerCode,
            Integer referralsCount,
            Boolean isEmailConfirmed
    ) {}

    private static boolean hasGraphQlErrors(JsonNode json) {
        if (json == null) return false;

        JsonNode errors = json.get("errors");
        return errors != null && errors.isArray() && errors.size() > 0;
    }
    private record GraphQlCallResult(int httpStatus, String rawBody, JsonNode json) {}

    public static class WhiteMarketException extends RuntimeException {
        public WhiteMarketException(String message) { super(message); }
        public WhiteMarketException(String message, Throwable cause) { super(message, cause); }
    }

    public static class UnauthorizedException extends WhiteMarketException {
        public UnauthorizedException(String message) { super(message); }
    }

    public record LowestSell(double priceUsd, String productId) {}

    public record EditOfferResult(
            int offerIndex,
            String offerDbId,     // Offer.id (як строка) - зручно логувати
            String productType,   // __typename
            String productId,
            String slug,          // може бути null (для history)
            String priceValue,    // може бути null
            String priceCurrency, // може бути null
            String errorCategory,
            String errorMessage
    ) {}

    public record SoldCheck(
            boolean sold,
            String dealId,
            String soldAt,      // createdAt/archivedAt (як віддасть API)
            String productId,
            String assetId,
            double priceUsd
    ) {}

    public record DealHistoryHit(
            String dealHistoryId,
            String status,
            String productId,
            String productSlug,
            double priceUsd,
            String createdAt
    ) {}

    public record DealHistoryPage(List<DealHistoryItem> items, PageInfo pageInfo, Integer totalCount) {}

    public record DealHistoryItem(
            String dealId,
            String customId,
            String createdAt, // якщо поле є в схемі (у прикладі фільтр сортується по CREATED_AT)
            PersonPublic seller,
            PersonPublic buyer,
            ProductHistory product
    ) {}

    public record PersonPublic(
            String id,
            String steamId,
            String steamName
    ) {}

    public record ProductHistory(
            String id,          // productId, який тобі і треба
            Integer appId,
            Integer contextId,
            String assetId,
            Money price,
            String status,
            String archivedAt,
            SteamItemDesc description
    ) {}

    public record SteamItemDesc(
            String nameHash,
            String name,
            String icon,
            String iconLarge
    ) {}

    public record Money(String value, String currency) {}

}
