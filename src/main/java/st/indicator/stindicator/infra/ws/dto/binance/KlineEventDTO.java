package st.indicator.stindicator.infra.ws.dto.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KlineEventDTO {

    @JsonProperty("e")
    private String eventType;

    @JsonProperty("E")
    private long eventTime;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("k")
    private KlineData kline;

    public KlineEventDTO(String eventType, long eventTime, String symbol, KlineData kline) {
        this.eventType = eventType;
        this.eventTime = eventTime;
        this.symbol = symbol;
        this.kline = kline;
    }

    public String getEventType() {
        return eventType;
    }

    public long getEventTime() {
        return eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public KlineData getKline() {
        return kline;
    }

    @Override
    public String toString() {
        return "KlineEventDTO{" +
                "eventType='" + eventType + '\'' +
                ", eventTime=" + eventTime +
                ", symbol='" + symbol + '\'' +
                ", kline=" + kline +
                '}';
    }
}