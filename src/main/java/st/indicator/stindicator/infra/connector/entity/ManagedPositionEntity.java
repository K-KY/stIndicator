package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.*;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.ManagedPositionCloseReason;
import st.indicator.stindicator.domain.entity.ManagedPositionStatus;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// Binance TP/SL 주문 없이 서비스 내부 가격 감시로 청산 조건을 평가하는 포지션 상태 엔티티
@Entity
@Table(name = "managed_position")
public class ManagedPositionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private String entrySide;
    private String closeSide;
    private String entryOrderId;
    private String closeOrderId;
    @Column(precision = 38, scale = 18)
    private BigDecimal entryPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal quantity;
    @Column(precision = 38, scale = 18)
    private BigDecimal initialStopPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal currentStopPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal targetPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal possibleLoss;
    @Column(precision = 38, scale = 18)
    private BigDecimal possibleProfit;
    @Column(precision = 38, scale = 18)
    private BigDecimal leverage;
    @Column(precision = 38, scale = 18)
    private BigDecimal requiredMargin;
    @Enumerated(EnumType.STRING)
    private TriggerBasis stopTriggerBasis;
    @Enumerated(EnumType.STRING)
    private TriggerBasis takeProfitTriggerBasis;
    @Enumerated(EnumType.STRING)
    private ManagedOrderMode mode;
    private boolean raiseStopEnabled;
    @Enumerated(EnumType.STRING)
    private RaiseStopType raiseTriggerType;
    @Column(precision = 38, scale = 18)
    private BigDecimal raiseTriggerValue;
    @Enumerated(EnumType.STRING)
    private RaiseStopType raiseStopType;
    @Column(precision = 38, scale = 18)
    private BigDecimal raiseStopValue;
    private boolean raiseActivated;
    @Column(precision = 38, scale = 18)
    private BigDecimal highestPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal lowestPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal currentPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal realizedPnl;
    @Column(precision = 38, scale = 18)
    private BigDecimal unrealizedPnl;
    @Enumerated(EnumType.STRING)
    private ManagedPositionStatus status;
    @Enumerated(EnumType.STRING)
    private ManagedPositionCloseReason closeReason;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime updatedAt;

    public ManagedPositionEntity() {
    }

    public static ManagedPositionEntity from(PendingOrderEntity pending, BigDecimal fillPrice) {
        ManagedPositionEntity entity = new ManagedPositionEntity();
        entity.userId = pending.getUserId();
        entity.symbol = pending.getSymbol();
        entity.entrySide = pending.getSide();
        entity.closeSide = "BUY".equalsIgnoreCase(pending.getSide()) ? "SELL" : "BUY";
        entity.entryOrderId = pending.getOrderId();
        entity.entryPrice = fillPrice == null || fillPrice.signum() == 0 ? pending.getEntryPrice() : fillPrice;
        entity.quantity = pending.getQuantity();
        entity.initialStopPrice = pending.getStopPrice();
        entity.currentStopPrice = pending.getStopPrice();
        entity.possibleLoss = pending.getPossibleLoss();
        entity.leverage = pending.getLeverage();
        entity.requiredMargin = pending.getRequiredMargin();
        entity.stopTriggerBasis = pending.getStopTriggerBasis();
        entity.mode = pending.getMode();
        if (entity.mode == ManagedOrderMode.RAISING_STOP_ONLY) {
            entity.targetPrice = null;
            entity.possibleProfit = null;
            entity.takeProfitTriggerBasis = null;
        } else {
            entity.targetPrice = pending.getTargetPrice();
            entity.possibleProfit = pending.getPossibleProfit();
            entity.takeProfitTriggerBasis = pending.getTakeProfitTriggerBasis();
        }
        entity.raiseStopEnabled = pending.isRaiseStopEnabled();
        entity.raiseTriggerType = pending.getRaiseTriggerType();
        entity.raiseTriggerValue = pending.getRaiseTriggerValue();
        entity.raiseStopType = pending.getRaiseStopType();
        entity.raiseStopValue = pending.getRaiseStopValue();
        entity.raiseActivated = false;
        entity.highestPrice = entity.entryPrice;
        entity.lowestPrice = entity.entryPrice;
        entity.currentPrice = entity.entryPrice;
        entity.realizedPnl = BigDecimal.ZERO;
        entity.unrealizedPnl = BigDecimal.ZERO;
        entity.status = ManagedPositionStatus.ACTIVE;
        entity.openedAt = LocalDateTime.now();
        entity.updatedAt = entity.openedAt;
        return entity;
    }

    public void updateMarket(BigDecimal currentPrice, BigDecimal unrealizedPnl,
                             BigDecimal highestPrice, BigDecimal lowestPrice,
                             BigDecimal currentStopPrice, boolean raiseActivated) {
        this.currentPrice = currentPrice;
        this.unrealizedPnl = unrealizedPnl;
        this.highestPrice = highestPrice;
        this.lowestPrice = lowestPrice;
        this.currentStopPrice = currentStopPrice;
        this.raiseActivated = raiseActivated;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isLegacyManagedPolicy() {
        if (mode == null || stopTriggerBasis == null || initialStopPrice == null
                || currentStopPrice == null
                || possibleLoss == null
        ) {
            return true;
        }
        return mode != ManagedOrderMode.RAISING_STOP_ONLY
                && (takeProfitTriggerBasis == null || targetPrice == null || possibleProfit == null);
    }

    public void applyLegacyFixedPolicy() {
        BigDecimal onePercent = new BigDecimal("0.01");
        boolean longSide = "BUY".equalsIgnoreCase(entrySide);
        BigDecimal stop = longSide
                ? entryPrice.multiply(BigDecimal.ONE.subtract(onePercent))
                : entryPrice.multiply(BigDecimal.ONE.add(onePercent));
        BigDecimal target = longSide
                ? entryPrice.multiply(BigDecimal.ONE.add(onePercent))
                : entryPrice.multiply(BigDecimal.ONE.subtract(onePercent));
        BigDecimal quantityValue = quantity == null ? BigDecimal.ZERO : quantity;
        this.initialStopPrice = stop;
        this.currentStopPrice = stop;
        this.targetPrice = target;
        this.possibleLoss = stop.subtract(entryPrice).abs().multiply(quantityValue).setScale(8, RoundingMode.HALF_UP);
        this.possibleProfit = target.subtract(entryPrice).abs().multiply(quantityValue).setScale(8, RoundingMode.HALF_UP);
        this.stopTriggerBasis = TriggerBasis.PRICE_PERCENT;
        this.takeProfitTriggerBasis = TriggerBasis.PRICE_PERCENT;
        this.mode = ManagedOrderMode.FIXED_TP_SL;
        this.raiseStopEnabled = false;
        this.raiseActivated = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void markClosing(String closeOrderId, ManagedPositionCloseReason reason) {
        this.status = ManagedPositionStatus.CLOSING;
        this.closeOrderId = closeOrderId;
        this.closeReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    public void markClosed(BigDecimal realizedPnl) {
        this.status = ManagedPositionStatus.CLOSED;
        this.realizedPnl = realizedPnl == null ? this.unrealizedPnl : realizedPnl;
        this.closedAt = LocalDateTime.now();
        this.updatedAt = this.closedAt;
    }

    public void markFailed() {
        this.status = ManagedPositionStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getEntrySide() { return entrySide; }
    public String getCloseSide() { return closeSide; }
    public String getEntryOrderId() { return entryOrderId; }
    public String getCloseOrderId() { return closeOrderId; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getInitialStopPrice() { return initialStopPrice; }
    public BigDecimal getCurrentStopPrice() { return currentStopPrice; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public BigDecimal getPossibleLoss() { return possibleLoss; }
    public BigDecimal getPossibleProfit() { return possibleProfit; }
    public BigDecimal getLeverage() { return leverage; }
    public BigDecimal getRequiredMargin() { return requiredMargin; }
    public TriggerBasis getStopTriggerBasis() { return stopTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : stopTriggerBasis; }
    public TriggerBasis getTakeProfitTriggerBasis() {
        if (getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            return null;
        }
        return takeProfitTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : takeProfitTriggerBasis;
    }
    public ManagedOrderMode getMode() { return mode == null ? ManagedOrderMode.FIXED_TP_SL : mode; }
    public boolean isRaiseStopEnabled() { return raiseStopEnabled; }
    public RaiseStopType getRaiseTriggerType() { return raiseTriggerType; }
    public BigDecimal getRaiseTriggerValue() { return raiseTriggerValue; }
    public RaiseStopType getRaiseStopType() { return raiseStopType; }
    public BigDecimal getRaiseStopValue() { return raiseStopValue; }
    public boolean isRaiseActivated() { return raiseActivated; }
    public BigDecimal getHighestPrice() { return highestPrice; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public ManagedPositionStatus getStatus() { return status; }
    public ManagedPositionCloseReason getCloseReason() { return closeReason; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
