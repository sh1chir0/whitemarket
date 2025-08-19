package ua.sh1chiro.Bot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by Sh1chiro on 19.08.2025.
 * <p>
 * When I wrote this code, only god and
 * I knew how it worked.
 * Now, only god knows it!
 *
 * @author Sh1chiro
 */

public class TargetImportItem {
    private String host;

    @JsonProperty("hash_name")
    private String hashName;

    @JsonProperty("max_price")
    private String maxPrice;

    @JsonProperty("min_price")
    private String minPrice;

    private Integer count;

    @JsonProperty("trade_url")
    private String tradeUrl;

    // getters/setters
    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getHashName() { return hashName; }
    public void setHashName(String hashName) { this.hashName = hashName; }

    public String getMaxPrice() { return maxPrice; }
    public void setMaxPrice(String maxPrice) { this.maxPrice = maxPrice; }

    public String getMinPrice() { return minPrice; }
    public void setMinPrice(String minPrice) { this.minPrice = minPrice; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public String getTradeUrl() { return tradeUrl; }
    public void setTradeUrl(String tradeUrl) { this.tradeUrl = tradeUrl; }
}
