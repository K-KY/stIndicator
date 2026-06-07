package st.indicator.stindicator.presentation.ws.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Binance mark price WebSocket 메시지 DTO")
public class BinanceMarkPriceMessage {
    @Schema(description = "이벤트 타입")
    @JsonProperty("e")
    private String eventType;

    @Schema(description = "이벤트 발생 시각")
    @JsonProperty("E")
    private long eventTime;

    @Schema(description = "심볼")
    @JsonProperty("s")
    private String symbol;

    @Schema(description = "마크 가격")
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
