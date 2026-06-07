package st.indicator.stindicator.presentation.ws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

//
public class BinanceMarkPriceMessage {
    @JsonProperty("e")
    private String eventType;

    @JsonProperty("E")
    private long eventTime;

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private BigDecimal markPrice;

    public String getEventType() {
        return eventType;
    }

    public long getEventTime() {
        return eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getMarkPrice() {
        return markPrice;
    }
}
