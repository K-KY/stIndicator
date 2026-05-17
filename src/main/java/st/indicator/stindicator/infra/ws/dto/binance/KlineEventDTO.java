package st.indicator.stindicator.infra.ws.dto.binance;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KlineEventDTO {

    // Binance websocket 이벤트 타입. kline 이벤트인지 식별한다
    @JsonProperty("e")
    private String eventType;

    // 이벤트가 생성된 거래소 시각
    @JsonProperty("E")
    private long eventTime;

    // 이벤트가 발생한 심볼
    @JsonProperty("s")
    private String symbol;

    // 실제 캔들 상세 데이터
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
