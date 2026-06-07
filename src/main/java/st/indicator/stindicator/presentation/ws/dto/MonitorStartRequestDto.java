package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.MonitorOrderType;
import st.indicator.stindicator.domain.entity.PositionDirection;

import java.math.BigDecimal;

@Schema(description = "포지션 모니터링 시작 요청 DTO")
public class MonitorStartRequestDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "모니터링할 심볼", example = "BTCUSDT")
    private String symbol;
    @Schema(description = "포지션 방향", example = "BUY")
    private PositionDirection direction;
    @Schema(description = "진입 가격", example = "60000")
    private BigDecimal entryPrice;
    @Schema(description = "수량", example = "0.01")
    private BigDecimal quantity;
    @Schema(description = "레버리지", example = "10")
    private BigDecimal leverage;
    @Schema(description = "트레일링 스탑 갭 비율", example = "3")
    private BigDecimal trailingGapPercent;
    @Schema(description = "청산 주문 타입", example = "MARKET")
    private MonitorOrderType closeOrderType;
    @Schema(description = "지정가 청산 시 사용할 가격", example = "62000")
    private BigDecimal closeLimitPrice;

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

    public PositionDirection getDirection() {
        return direction;
    }

    public void setDirection(PositionDirection direction) {
        this.direction = direction;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    public BigDecimal getTrailingGapPercent() {
        return trailingGapPercent;
    }

    public void setTrailingGapPercent(BigDecimal trailingGapPercent) {
        this.trailingGapPercent = trailingGapPercent;
    }

    public MonitorOrderType getCloseOrderType() {
        return closeOrderType;
    }

    public void setCloseOrderType(MonitorOrderType closeOrderType) {
        this.closeOrderType = closeOrderType;
    }

    public BigDecimal getCloseLimitPrice() {
        return closeLimitPrice;
    }

    public void setCloseLimitPrice(BigDecimal closeLimitPrice) {
        this.closeLimitPrice = closeLimitPrice;
    }
}
