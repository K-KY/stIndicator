package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.*;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.PendingOrderStatus;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;
import st.indicator.stindicator.domain.entity.TradeExecutionMode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 서비스가 직접 TP/SL을 관리하기 위해 Binance LIMIT 진입 주문의 체결 전 상태를 저장하는 엔티티
@Entity
@Table(name = "pending_order")
public class PendingOrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String symbol;
    private String side;
    private String orderId;
    private String clientOrderId;
    @Column(precision = 38, scale = 8)
    private BigDecimal entryPrice;
    @Column(precision = 38, scale = 8)
    private BigDecimal quantity;
    @Column(precision = 38, scale = 8)
    private BigDecimal stopPrice;
    @Column(precision = 38, scale = 8)
    private BigDecimal targetPrice;
    @Column(precision = 38, scale = 8)
    private BigDecimal possibleLoss;
    @Column(precision = 38, scale = 8)
    private BigDecimal possibleProfit;
    @Column(precision = 38)
    private BigDecimal leverage;
    @Column(precision = 38, scale = 8)
    private BigDecimal requiredMargin;
    @Enumerated(EnumType.STRING)
    private TriggerBasis stopTriggerBasis;
    @Enumerated(EnumType.STRING)
    private TriggerBasis takeProfitTriggerBasis;
    @Column(precision = 38, scale = 8)
    private BigDecimal atr;
    @Column(precision = 38)
    private BigDecimal atrMultiplier;
    @Column(precision = 38)
    private BigDecimal riskPercent;
    @Enumerated(EnumType.STRING)
    private ManagedOrderMode mode;
    private boolean raiseStopEnabled;
    @Enumerated(EnumType.STRING)
    private RaiseStopType raiseTriggerType;
    @Column(precision = 38, scale = 8)
    private BigDecimal raiseTriggerValue;
    @Enumerated(EnumType.STRING)
    private RaiseStopType raiseStopType;
    @Column(precision = 38, scale = 8)
    private BigDecimal raiseStopValue;
    @Enumerated(EnumType.STRING)
    private TradeExecutionMode executionMode;
    @Enumerated(EnumType.STRING)
    private PendingOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public PendingOrderEntity() {
    }

    public static PendingOrderEntity create(Long userId, String symbol, String side, String orderId,
                                            String clientOrderId, BigDecimal entryPrice, BigDecimal quantity,
                                            BigDecimal stopPrice, BigDecimal targetPrice, BigDecimal possibleLoss,
                                            BigDecimal possibleProfit, BigDecimal leverage, BigDecimal requiredMargin,
                                            TriggerBasis stopTriggerBasis, TriggerBasis takeProfitTriggerBasis,
                                            BigDecimal atr, BigDecimal atrMultiplier, BigDecimal riskPercent,
                                            ManagedOrderMode mode, boolean raiseStopEnabled,
                                            RaiseStopType raiseTriggerType, BigDecimal raiseTriggerValue,
                                            RaiseStopType raiseStopType, BigDecimal raiseStopValue,
                                            TradeExecutionMode executionMode) {
        PendingOrderEntity entity = new PendingOrderEntity();
        entity.userId = userId;
        entity.symbol = symbol;
        entity.side = side;
        entity.orderId = orderId;
        entity.clientOrderId = clientOrderId;
        entity.entryPrice = entryPrice;
        entity.quantity = quantity;
        entity.stopPrice = stopPrice;
        entity.possibleLoss = possibleLoss;
        entity.leverage = leverage;
        entity.requiredMargin = requiredMargin;
        entity.stopTriggerBasis = stopTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : stopTriggerBasis;
        entity.atr = atr;
        entity.atrMultiplier = atrMultiplier;
        entity.riskPercent = riskPercent;
        entity.mode = mode == null ? ManagedOrderMode.FIXED_TP_SL : mode;
        entity.raiseStopEnabled = entity.mode == ManagedOrderMode.RAISING_STOP_ONLY && raiseStopEnabled;
        if (entity.mode == ManagedOrderMode.RAISING_STOP_ONLY) {
            entity.targetPrice = null;
            entity.possibleProfit = null;
            entity.takeProfitTriggerBasis = null;
        } else {
            entity.targetPrice = targetPrice;
            entity.possibleProfit = possibleProfit;
            entity.takeProfitTriggerBasis = takeProfitTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : takeProfitTriggerBasis;
        }
        entity.raiseTriggerType = raiseTriggerType;
        entity.raiseTriggerValue = raiseTriggerValue;
        entity.raiseStopType = raiseStopType;
        entity.raiseStopValue = raiseStopValue;
        entity.executionMode = executionMode == null ? TradeExecutionMode.REAL : executionMode;
        entity.status = PendingOrderStatus.PENDING;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = entity.createdAt;
        return entity;
    }

    public void mark(PendingOrderStatus status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public void clearTakeProfit() {
        this.targetPrice = null;
        this.possibleProfit = null;
        this.takeProfitTriggerBasis = null;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateConditions(BigDecimal stopPrice, BigDecimal targetPrice,
                                 BigDecimal possibleLoss, BigDecimal possibleProfit,
                                 TriggerBasis stopTriggerBasis, TriggerBasis takeProfitTriggerBasis,
                                 RaiseStopType raiseTriggerType, BigDecimal raiseTriggerValue,
                                 RaiseStopType raiseStopType, BigDecimal raiseStopValue) {
        this.stopPrice = stopPrice == null ? this.stopPrice : stopPrice;
        this.targetPrice = targetPrice == null ? this.targetPrice : targetPrice;
        this.possibleLoss = possibleLoss == null ? this.possibleLoss : possibleLoss;
        this.possibleProfit = possibleProfit == null ? this.possibleProfit : possibleProfit;
        this.stopTriggerBasis = stopTriggerBasis == null ? this.stopTriggerBasis : stopTriggerBasis;
        this.takeProfitTriggerBasis = takeProfitTriggerBasis == null ? this.takeProfitTriggerBasis : takeProfitTriggerBasis;
        this.raiseTriggerType = raiseTriggerType == null ? this.raiseTriggerType : raiseTriggerType;
        this.raiseTriggerValue = raiseTriggerValue == null ? this.raiseTriggerValue : raiseTriggerValue;
        this.raiseStopType = raiseStopType == null ? this.raiseStopType : raiseStopType;
        this.raiseStopValue = raiseStopValue == null ? this.raiseStopValue : raiseStopValue;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasInvalidSizing() {
        return quantity == null
                || quantity.signum() <= 0
                || requiredMargin == null
                || requiredMargin.signum() <= 0;
    }

    public boolean hasTakeProfitValues() {
        return targetPrice != null || possibleProfit != null || takeProfitTriggerBasis != null;
    }

    public void repairSizing(BigDecimal quantity, BigDecimal requiredMargin) {
        this.quantity = quantity == null ? this.quantity : quantity;
        this.requiredMargin = requiredMargin == null ? this.requiredMargin : requiredMargin;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public String getOrderId() { return orderId; }
    public String getClientOrderId() { return clientOrderId; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getStopPrice() { return stopPrice; }
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
    public BigDecimal getAtr() { return atr; }
    public BigDecimal getAtrMultiplier() { return atrMultiplier; }
    public BigDecimal getRiskPercent() { return riskPercent; }
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
    public PendingOrderStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
