package st.indicator.stindicator.infra.ws.dto.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KlineData {

    @JsonProperty("t")
    private long openTime;

    @JsonProperty("T")
    private long closeTime;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("i")
    private String interval;

    @JsonProperty("f")
    private long firstTradeId;

    @JsonProperty("L")
    private long lastTradeId;

    @JsonProperty("o")
    private String openPrice;

    @JsonProperty("c")
    private String closePrice;

    @JsonProperty("h")
    private String highPrice;

    @JsonProperty("l")
    private String lowPrice;

    @JsonProperty("v")
    private String volume;

    @JsonProperty("n")
    private int tradeCount;

    @JsonProperty("x")
    private boolean closed; // 캔들 확정 여부

    @JsonProperty("q")
    private String quoteAssetVolume;

    @JsonProperty("V")
    private String takerBuyBaseVolume;

    @JsonProperty("Q")
    private String takerBuyQuoteVolume;

    @JsonProperty("B")
    private String ignore;

    public KlineData(long openTime, long closeTime, String symbol, String interval, long firstTradeId,
                     long lastTradeId, String openPrice, String closePrice, String highPrice, String lowPrice,
                     String volume, int tradeCount, boolean closed, String quoteAssetVolume, String takerBuyBaseVolume,
                     String takerBuyQuoteVolume, String ignore) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.symbol = symbol;
        this.interval = interval;
        this.firstTradeId = firstTradeId;
        this.lastTradeId = lastTradeId;
        this.openPrice = openPrice;
        this.closePrice = closePrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
        this.tradeCount = tradeCount;
        this.closed = closed;
        this.quoteAssetVolume = quoteAssetVolume;
        this.takerBuyBaseVolume = takerBuyBaseVolume;
        this.takerBuyQuoteVolume = takerBuyQuoteVolume;
        this.ignore = ignore;
    }

    public long getOpenTime() {
        return openTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public long getFirstTradeId() {
        return firstTradeId;
    }

    public long getLastTradeId() {
        return lastTradeId;
    }

    public String getOpenPrice() {
        return openPrice;
    }

    public String getClosePrice() {
        return closePrice;
    }

    public String getHighPrice() {
        return highPrice;
    }

    public String getLowPrice() {
        return lowPrice;
    }

    public String getVolume() {
        return volume;
    }

    public int getTradeCount() {
        return tradeCount;
    }

    public boolean isClosed() {
        return closed;
    }

    public String getQuoteAssetVolume() {
        return quoteAssetVolume;
    }

    public String getTakerBuyBaseVolume() {
        return takerBuyBaseVolume;
    }

    public String getTakerBuyQuoteVolume() {
        return takerBuyQuoteVolume;
    }

    public String getIgnore() {
        return ignore;
    }
}