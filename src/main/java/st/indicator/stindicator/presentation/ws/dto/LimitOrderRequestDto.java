package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.PositionDirection;

import java.math.BigDecimal;

@Schema(description = "지정가 주문 요청 DTO")
public class LimitOrderRequestDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "주문 심볼", example = "BTCUSDT")
    private String symbol;
    @Schema(description = "주문 방향", example = "SELL")
    private PositionDirection side;
    @Schema(description = "주문 수량", example = "0.01")
    private BigDecimal quantity;
    @Schema(description = "지정가", example = "62000")
    private BigDecimal price;
    @Schema(description = "주문 유지 정책", example = "GTC")
    private String timeInForce;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public PositionDirection getSide() {
        return side;
    }

    public void setSide(PositionDirection side) {
        this.side = side;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }
}
