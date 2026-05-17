package st.indicator.stindicator.infra.ws.dto.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KlineData {

    // 캔들 시작 시각
    @JsonProperty("t")
    private long openTime;

    // 캔들 종료 시각
    @JsonProperty("T")
    private long closeTime;

    // 해당 캔들의 심볼
    @JsonProperty("s")
    private String symbol;

    // 캔들 주기 예: 1m, 1h
    @JsonProperty("i")
    private String interval;

    // 캔들 구간의 첫 체결 ID
    @JsonProperty("f")
    private long firstTradeId;

    // 캔들 구간의 마지막 체결 ID
    @JsonProperty("L")
    private long lastTradeId;

    // 시가
    @JsonProperty("o")
    private String openPrice;

    // 종가
    @JsonProperty("c")
    private String closePrice;

    // 고가
    @JsonProperty("h")
    private String highPrice;

    // 저가
    @JsonProperty("l")
    private String lowPrice;

    // 거래량(base asset volume)
    @JsonProperty("v")
    private String volume;

    // 구간 내 체결 수
    @JsonProperty("n")
    private int tradeCount;

    // 캔들이 확정되어 더 이상 값이 바뀌지 않는지 여부
    @JsonProperty("x")
    private boolean closed;

    // quote asset 기준 거래량
    @JsonProperty("q")
    private String quoteAssetVolume;

    // taker buy 기준 base asset 거래량
    @JsonProperty("V")
    private String takerBuyBaseVolume;

    // taker buy 기준 quote asset 거래량
    @JsonProperty("Q")
    private String takerBuyQuoteVolume;

    // Binance 명세상 사용하지 않는 예약 필드
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
