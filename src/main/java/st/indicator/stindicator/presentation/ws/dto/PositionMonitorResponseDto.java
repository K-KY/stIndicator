package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.MonitorOrderType;
import st.indicator.stindicator.domain.entity.PositionDirection;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "포지션 모니터링 상태 응답 DTO")
public class PositionMonitorResponseDto {
    @Schema(description = "모니터링 ID")
    private Long id;
    @Schema(description = "사용자 ID")
    private Long userId;
    @Schema(description = "심볼")
    private String symbol;
    @Schema(description = "포지션 방향")
    private PositionDirection direction;
    @Schema(description = "진입 가격")
    private BigDecimal entryPrice;
    @Schema(description = "현재 가격")
    private BigDecimal currentPrice;
    @Schema(description = "수량")
    private BigDecimal quantity;
    @Schema(description = "레버리지")
    private BigDecimal leverage;
    @Schema(description = "현재 수익률")
    private BigDecimal currentProfitPercent;
    @Schema(description = "최고 수익률")
    private BigDecimal highestProfitPercent;
    @Schema(description = "현재 트레일링 스탑 값")
    private BigDecimal trailingStopPercent;
    @Schema(description = "현재 스탑 라인")
    private BigDecimal currentStopLine;
    @Schema(description = "트레일링 갭")
    private BigDecimal trailingGapPercent;
    @Schema(description = "청산 주문 타입")
    private MonitorOrderType closeOrderType;
    @Schema(description = "청산 지정가")
    private BigDecimal closeLimitPrice;
    @Schema(description = "상태")
    private PositionMonitorStatus status;
    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;
    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static PositionMonitorResponseDto from(PositionMonitor positionMonitor) {
        PositionMonitorResponseDto dto = new PositionMonitorResponseDto();
        dto.id = positionMonitor.getId();
        dto.userId = positionMonitor.getUserId();
        dto.symbol = positionMonitor.getSymbol();
        dto.direction = positionMonitor.getDirection();
        dto.entryPrice = positionMonitor.getEntryPrice();
        dto.currentPrice = positionMonitor.getCurrentPrice();
        dto.quantity = positionMonitor.getQuantity();
        dto.leverage = positionMonitor.getLeverage();
        dto.currentProfitPercent = positionMonitor.getCurrentProfitPercent();
        dto.highestProfitPercent = positionMonitor.getHighestProfitPercent();
        dto.trailingStopPercent = positionMonitor.getTrailingStopPercent();
        dto.currentStopLine = positionMonitor.getCurrentStopLine();
        dto.trailingGapPercent = positionMonitor.getTrailingGapPercent();
        dto.closeOrderType = positionMonitor.getCloseOrderType();
        dto.closeLimitPrice = positionMonitor.getCloseLimitPrice();
        dto.status = positionMonitor.getStatus();
        dto.createdAt = positionMonitor.getCreatedAt();
        dto.updatedAt = positionMonitor.getUpdatedAt();
        return dto;
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
