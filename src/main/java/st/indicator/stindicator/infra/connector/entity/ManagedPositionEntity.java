package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.*;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.ManagedPositionCloseReason;
import st.indicator.stindicator.domain.entity.ManagedPositionStatus;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;
import st.indicator.stindicator.domain.entity.TradeExecutionMode;

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
    @Column(precision = 38, scale = 18)
    private BigDecimal atr;
    @Column(precision = 38, scale = 18)
    private BigDecimal atrMultiplier;
    @Column(precision = 38, scale = 18)
    private BigDecimal riskPercent;
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
    @Enumerated(EnumType.STRING)
    private TradeExecutionMode executionMode;
    private boolean raiseActivated;
    @Column(precision = 38, scale = 18)
    private BigDecimal highestPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal lowestPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal currentPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal closePrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal realizedPnl;
    @Column(precision = 38, scale = 18)
    private BigDecimal unrealizedPnl;
    @Enumerated(EnumType.STRING)
    private ManagedPositionStatus status;
    @Enumerated(EnumType.STRING)
    private ManagedPositionCloseReason closeReason;
    @Lob
    private String managementEvents;
    private LocalDateTime openedAt;
    private LocalDateTime closedAt;
    private LocalDateTime updatedAt;

    public ManagedPositionEntity() {
    }

    public static ManagedPositionEntity from(PendingOrderEntity pending, BigDecimal fillPrice, BigDecimal executedQuantity) {
        ManagedPositionEntity entity = new ManagedPositionEntity();
        entity.userId = pending.getUserId();
        entity.symbol = pending.getSymbol();
        entity.entrySide = pending.getSide();
        entity.closeSide = "BUY".equalsIgnoreCase(pending.getSide()) ? "SELL" : "BUY";
        entity.entryOrderId = pending.getOrderId();
        entity.entryPrice = fillPrice == null || fillPrice.signum() == 0 ? pending.getEntryPrice() : fillPrice;
        entity.quantity = executedQuantity == null || executedQuantity.signum() <= 0
                ? pending.getQuantity()
                : executedQuantity;
        entity.leverage = pending.getLeverage();
        entity.requiredMargin = calculateRequiredMargin(entity.entryPrice, entity.quantity, entity.leverage);
        entity.atr = pending.getAtr();
        entity.atrMultiplier = pending.getAtrMultiplier();
        entity.riskPercent = pending.getRiskPercent();
        entity.stopTriggerBasis = pending.getStopTriggerBasis();
        entity.mode = pending.getMode();
        entity.executionMode = pending.getExecutionMode();
        entity.applyActualFillExitPlan(pending);
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
        entity.appendEvent("ENTRY_FILLED symbol=" + entity.symbol + ", entryOrderId=" + entity.entryOrderId
                + ", entryPrice=" + entity.entryPrice + ", quantity=" + entity.quantity);
        entity.appendEvent("MONITORING_STARTED mode=" + entity.getMode()
                + ", executionMode=" + entity.getExecutionMode()
                + ", stopBasis=" + entity.getStopTriggerBasis()
                + ", raiseTriggerType=" + entity.raiseTriggerType
                + ", raiseTriggerValue=" + entity.raiseTriggerValue
                + ", raiseStopType=" + entity.raiseStopType
                + ", raiseStopValue=" + entity.raiseStopValue);
        return entity;
    }

    public static ManagedPositionEntity fromExternalPosition(Long userId, PositionRisk position) {
        if (position == null
                || position.getPositionAmt() == null
                || position.getPositionAmt().signum() == 0
                || position.getEntryPrice() == null
                || position.getEntryPrice().signum() <= 0) {
            throw new IllegalArgumentException("편입할 Binance 포지션의 수량과 진입가는 필수입니다.");
        }
        ManagedPositionEntity entity = new ManagedPositionEntity();
        boolean longSide = position.getPositionAmt().signum() > 0;
        BigDecimal onePercent = new BigDecimal("0.01");
        entity.userId = userId;
        entity.symbol = position.getSymbol();
        entity.entrySide = longSide ? "BUY" : "SELL";
        entity.closeSide = longSide ? "SELL" : "BUY";
        entity.entryOrderId = "EXTERNAL-" + position.getSymbol() + "-" + System.currentTimeMillis();
        entity.entryPrice = position.getEntryPrice();
        entity.quantity = position.getPositionAmt().abs();
        entity.leverage = valueOrDefault(position.getLeverage(), BigDecimal.ONE);
        entity.requiredMargin = calculateRequiredMargin(entity.entryPrice, entity.quantity, entity.leverage);
        entity.atr = null;
        entity.atrMultiplier = BigDecimal.ONE;
        entity.riskPercent = BigDecimal.ONE;
        entity.stopTriggerBasis = TriggerBasis.PRICE_PERCENT;
        entity.takeProfitTriggerBasis = null;
        entity.mode = ManagedOrderMode.RAISING_STOP_ONLY;
        entity.executionMode = TradeExecutionMode.REAL;
        entity.raiseStopEnabled = true;
        entity.raiseTriggerType = RaiseStopType.PERCENT;
        entity.raiseTriggerValue = BigDecimal.ONE;
        entity.raiseStopType = RaiseStopType.PERCENT;
        entity.raiseStopValue = new BigDecimal("50");
        entity.raiseActivated = false;
        entity.initialStopPrice = longSide
                ? entity.entryPrice.multiply(BigDecimal.ONE.subtract(onePercent))
                : entity.entryPrice.multiply(BigDecimal.ONE.add(onePercent));
        entity.currentStopPrice = entity.initialStopPrice;
        entity.targetPrice = null;
        entity.possibleLoss = entity.initialStopPrice.subtract(entity.entryPrice).abs().multiply(entity.quantity);
        entity.possibleProfit = null;
        entity.currentPrice = position.getMarkPrice() == null || position.getMarkPrice().signum() <= 0
                ? entity.entryPrice
                : position.getMarkPrice();
        entity.unrealizedPnl = position.getUnrealizedProfit() == null ? BigDecimal.ZERO : position.getUnrealizedProfit();
        entity.realizedPnl = BigDecimal.ZERO;
        entity.highestPrice = entity.entryPrice.max(entity.currentPrice);
        entity.lowestPrice = entity.entryPrice.min(entity.currentPrice);
        entity.status = ManagedPositionStatus.ACTIVE;
        entity.openedAt = LocalDateTime.now();
        entity.updatedAt = entity.openedAt;
        entity.appendEvent("EXTERNAL_POSITION_IMPORTED symbol=" + entity.symbol
                + ", side=" + entity.entrySide
                + ", entryPrice=" + entity.entryPrice
                + ", currentPrice=" + entity.currentPrice
                + ", quantity=" + entity.quantity
                + ", leverage=" + entity.leverage);
        entity.appendEvent("MONITORING_STARTED mode=" + entity.mode
                + ", executionMode=" + entity.executionMode
                + ", stopBasis=" + entity.stopTriggerBasis
                + ", raiseTriggerType=" + entity.raiseTriggerType
                + ", raiseTriggerValue=" + entity.raiseTriggerValue
                + ", raiseStopType=" + entity.raiseStopType
                + ", raiseStopValue=" + entity.raiseStopValue);
        return entity;
    }

    private void applyActualFillExitPlan(PendingOrderEntity pending) {
        boolean longSide = "BUY".equalsIgnoreCase(entrySide);
        BigDecimal riskRate = valueOrDefault(riskPercent, BigDecimal.ONE)
                .divide(new BigDecimal("100"), 18, RoundingMode.HALF_UP);
        BigDecimal atrDistance = pending.getAtr() == null || pending.getAtrMultiplier() == null
                ? pending.getEntryPrice().subtract(pending.getStopPrice()).abs()
                : pending.getAtr().multiply(pending.getAtrMultiplier());

        if (getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            this.possibleLoss = requiredMargin.multiply(riskRate);
            BigDecimal move = possibleLoss.divide(quantity, 18, RoundingMode.HALF_UP);
            this.initialStopPrice = longSide ? entryPrice.subtract(move) : entryPrice.add(move);
        } else {
            this.initialStopPrice = longSide ? entryPrice.subtract(atrDistance) : entryPrice.add(atrDistance);
            this.possibleLoss = atrDistance.multiply(quantity);
        }
        this.currentStopPrice = initialStopPrice;

        if (getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            this.targetPrice = null;
            this.possibleProfit = null;
            this.takeProfitTriggerBasis = null;
            return;
        }

        this.takeProfitTriggerBasis = pending.getTakeProfitTriggerBasis();
        if (getTakeProfitTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            this.possibleProfit = requiredMargin.multiply(riskRate);
            BigDecimal move = possibleProfit.divide(quantity, 18, RoundingMode.HALF_UP);
            this.targetPrice = longSide ? entryPrice.add(move) : entryPrice.subtract(move);
        } else {
            this.targetPrice = longSide ? entryPrice.add(atrDistance) : entryPrice.subtract(atrDistance);
            this.possibleProfit = atrDistance.multiply(quantity);
        }
    }

    public void updateTriggerBasis(TriggerBasis requestedStopBasis, TriggerBasis requestedTakeProfitBasis) {
        if (entryPrice == null || entryPrice.signum() <= 0
                || quantity == null || quantity.signum() <= 0
                || requiredMargin == null || requiredMargin.signum() <= 0) {
            throw new IllegalStateException("체결 기준 가격, 수량, 필요 증거금이 없어 트리거 기준을 변경할 수 없습니다.");
        }

        TriggerBasis previousStopBasis = getStopTriggerBasis();
        TriggerBasis previousTakeProfitBasis = getTakeProfitTriggerBasis();
        BigDecimal previousStopPrice = currentStopPrice;
        BigDecimal previousTargetPrice = targetPrice;
        boolean longSide = "BUY".equalsIgnoreCase(entrySide);
        BigDecimal riskRate = valueOrDefault(riskPercent, BigDecimal.ONE)
                .divide(new BigDecimal("100"), 18, RoundingMode.HALF_UP);
        BigDecimal referenceStopPrice = initialStopPrice == null ? currentStopPrice : initialStopPrice;
        if ((atr == null || atr.signum() <= 0 || atrMultiplier == null || atrMultiplier.signum() <= 0)
                && referenceStopPrice == null) {
            throw new IllegalStateException("ATR 또는 기존 손절가가 없어 트리거 기준을 변경할 수 없습니다.");
        }
        BigDecimal atrDistance = atr != null && atr.signum() > 0
                && atrMultiplier != null && atrMultiplier.signum() > 0
                ? atr.multiply(atrMultiplier)
                : entryPrice.subtract(referenceStopPrice).abs();

        if (requestedStopBasis != null) {
            stopTriggerBasis = requestedStopBasis;
            BigDecimal recalculatedStop;
            if (requestedStopBasis == TriggerBasis.PNL_PERCENT) {
                possibleLoss = requiredMargin.multiply(riskRate);
                BigDecimal priceMove = possibleLoss.divide(quantity, 18, RoundingMode.HALF_UP);
                recalculatedStop = longSide ? entryPrice.subtract(priceMove) : entryPrice.add(priceMove);
            } else {
                possibleLoss = atrDistance.multiply(quantity);
                recalculatedStop = longSide ? entryPrice.subtract(atrDistance) : entryPrice.add(atrDistance);
            }
            currentStopPrice = preserveRaisedStop(recalculatedStop, longSide);
        }

        if (getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            takeProfitTriggerBasis = null;
            targetPrice = null;
            possibleProfit = null;
        } else if (requestedTakeProfitBasis != null) {
            takeProfitTriggerBasis = requestedTakeProfitBasis;
            if (requestedTakeProfitBasis == TriggerBasis.PNL_PERCENT) {
                possibleProfit = requiredMargin.multiply(riskRate);
                BigDecimal priceMove = possibleProfit.divide(quantity, 18, RoundingMode.HALF_UP);
                targetPrice = longSide ? entryPrice.add(priceMove) : entryPrice.subtract(priceMove);
            } else {
                possibleProfit = atrDistance.multiply(quantity);
                targetPrice = longSide ? entryPrice.add(atrDistance) : entryPrice.subtract(atrDistance);
            }
        }

        appendEvent("TRIGGER_BASIS_UPDATED previousStopBasis=" + previousStopBasis
                + ", stopBasis=" + getStopTriggerBasis()
                + ", previousTakeProfitBasis=" + previousTakeProfitBasis
                + ", takeProfitBasis=" + getTakeProfitTriggerBasis()
                + ", previousStopPrice=" + previousStopPrice
                + ", currentStopPrice=" + currentStopPrice
                + ", previousTargetPrice=" + previousTargetPrice
                + ", targetPrice=" + targetPrice);
        updatedAt = LocalDateTime.now();
    }

    private BigDecimal preserveRaisedStop(BigDecimal recalculatedStop, boolean longSide) {
        if (getMode() != ManagedOrderMode.RAISING_STOP_ONLY || !raiseActivated || currentStopPrice == null) {
            return recalculatedStop;
        }
        if (longSide) {
            return currentStopPrice.max(recalculatedStop);
        }
        return currentStopPrice.min(recalculatedStop);
    }

    private static BigDecimal calculateRequiredMargin(BigDecimal entryPrice, BigDecimal quantity, BigDecimal leverage) {
        if (entryPrice == null || quantity == null || leverage == null || leverage.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return entryPrice.multiply(quantity).divide(leverage, 18, RoundingMode.HALF_UP);
    }

    private static BigDecimal valueOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value == null ? defaultValue : value;
    }

    public void appendEvent(String message) {
        String event = LocalDateTime.now() + " " + message;
        this.managementEvents = this.managementEvents == null || this.managementEvents.isBlank()
                ? event
                : this.managementEvents + "\n" + event;
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
        appendEvent("CLOSE_ORDER_SENT reason=" + reason + ", closeOrderId=" + closeOrderId);
        this.updatedAt = LocalDateTime.now();
    }

    public void markClosed(BigDecimal realizedPnl) {
        markClosed(realizedPnl, currentPrice);
    }

    public void markClosed(BigDecimal realizedPnl, BigDecimal closePrice) {
        this.status = ManagedPositionStatus.CLOSED;
        this.realizedPnl = realizedPnl == null ? this.unrealizedPnl : realizedPnl;
        this.closePrice = closePrice == null ? this.currentPrice : closePrice;
        this.closedAt = LocalDateTime.now();
        appendEvent("CLOSE_FILLED closePrice=" + this.closePrice + ", realizedPnl=" + this.realizedPnl
                + ", reason=" + this.closeReason);
        this.updatedAt = this.closedAt;
    }

    public void markTestClosed(ManagedPositionCloseReason reason, BigDecimal realizedPnl, BigDecimal closePrice) {
        this.closeReason = reason;
        this.closeOrderId = "TEST-CLOSE-" + id;
        appendEvent("TEST_CLOSE_TRIGGERED reason=" + reason + ", closePrice=" + closePrice);
        markClosed(realizedPnl, closePrice);
    }

    public void markFailed() {
        this.status = ManagedPositionStatus.FAILED;
        appendEvent("ERROR status=FAILED, reason=" + this.closeReason);
        this.updatedAt = LocalDateTime.now();
    }

    public void restoreActiveAfterCloseFailure(String message) {
        appendEvent("CLOSE_ORDER_FAILED message=" + message);
        this.status = ManagedPositionStatus.ACTIVE;
        this.closeOrderId = null;
        this.closeReason = null;
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
    public BigDecimal getAtr() { return atr; }
    public BigDecimal getAtrMultiplier() { return atrMultiplier; }
    public BigDecimal getRiskPercent() { return riskPercent; }
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
    public TradeExecutionMode getExecutionMode() {
        return executionMode == null ? TradeExecutionMode.REAL : executionMode;
    }
    public boolean isTestOrder() { return getExecutionMode() == TradeExecutionMode.TEST; }
    public boolean isRaiseActivated() { return raiseActivated; }
    public BigDecimal getHighestPrice() { return highestPrice; }
    public BigDecimal getLowestPrice() { return lowestPrice; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getClosePrice() { return closePrice; }
    public BigDecimal getRealizedPnl() { return realizedPnl; }
    public BigDecimal getUnrealizedPnl() { return unrealizedPnl; }
    public ManagedPositionStatus getStatus() { return status; }
    public ManagedPositionCloseReason getCloseReason() { return closeReason; }
    public String getManagementEvents() { return managementEvents; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
