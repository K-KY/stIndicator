package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 사용자가 보유 중인 포지션의 트레일링 스탑 상태를 영속화하고 복구하기 위한 도메인 엔티티다.
 * 서버 재시작 이후에도 highest watermark, stop line, 청산 방식 등을 이어서 사용할 수 있다.
 */
public class PositionMonitor {
    private final Long id;
    private final Long userId;
    private final String symbol;
    private final PositionDirection direction;
    private final BigDecimal entryPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal quantity;
    private final BigDecimal leverage;
    private final BigDecimal currentProfitPercent;
    private final BigDecimal highestProfitPercent;
    private final BigDecimal trailingStopPercent;
    private final BigDecimal currentStopLine;
    private final BigDecimal trailingGapPercent;
    private final MonitorOrderType closeOrderType;
    private final BigDecimal closeLimitPrice;
    private final PositionMonitorStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public PositionMonitor(Long id, Long userId, String symbol, PositionDirection direction,
                           BigDecimal entryPrice, BigDecimal currentPrice, BigDecimal quantity,
                           BigDecimal leverage, BigDecimal currentProfitPercent,
                           BigDecimal highestProfitPercent, BigDecimal trailingStopPercent,
                           BigDecimal currentStopLine, BigDecimal trailingGapPercent,
                           MonitorOrderType closeOrderType, BigDecimal closeLimitPrice,
                           PositionMonitorStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.direction = direction;
        this.entryPrice = entryPrice;
        this.currentPrice = currentPrice;
        this.quantity = quantity;
        this.leverage = leverage;
        this.currentProfitPercent = currentProfitPercent;
        this.highestProfitPercent = highestProfitPercent;
        this.trailingStopPercent = trailingStopPercent;
        this.currentStopLine = currentStopLine;
        this.trailingGapPercent = trailingGapPercent;
        this.closeOrderType = closeOrderType;
        this.closeLimitPrice = closeLimitPrice;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public PositionDirection getDirection() {
        return direction;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public BigDecimal getCurrentProfitPercent() {
        return currentProfitPercent;
    }

    public BigDecimal getHighestProfitPercent() {
        return highestProfitPercent;
    }

    public BigDecimal getTrailingStopPercent() {
        return trailingStopPercent;
    }

    public BigDecimal getCurrentStopLine() {
        return currentStopLine;
    }

    public BigDecimal getTrailingGapPercent() {
        return trailingGapPercent;
    }

    public MonitorOrderType getCloseOrderType() {
        return closeOrderType;
    }

    public BigDecimal getCloseLimitPrice() {
        return closeLimitPrice;
    }

    public PositionMonitorStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
