package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.*;
import st.indicator.stindicator.application.service.ManagedRaiseStopCalculator;
import st.indicator.stindicator.domain.entity.ManagedStopUpdateReason;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// 관리 포지션의 손절선이 실제 변경된 시점과 계산 근거를 보존하는 이력 엔티티
@Entity
@Table(
        name = "managed_stop_history",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_managed_stop_history_position_stop",
                columnNames = {"managed_position_id", "new_stop_price"}
        )
)
public class ManagedStopHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "managed_position_id", nullable = false)
    private ManagedPositionEntity managedPosition;

    private Long userId;
    private String symbol;
    private String side;
    private LocalDateTime changedAt;

    @Column(precision = 38, scale = 18)
    private BigDecimal currentPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal previousStopPrice;
    @Column(name = "new_stop_price", precision = 38, scale = 18)
    private BigDecimal newStopPrice;

    @Enumerated(EnumType.STRING)
    private TriggerBasis triggerBasis;
    @Column(precision = 38, scale = 18)
    private BigDecimal triggerValue;
    @Column(precision = 38, scale = 18)
    private BigDecimal triggerPrice;
    @Column(precision = 38, scale = 18)
    private BigDecimal unrealizedPnl;
    @Column(precision = 38, scale = 18)
    private BigDecimal pnlPercent;
    @Column(precision = 38, scale = 18)
    private BigDecimal priceChangePercent;

    @Enumerated(EnumType.STRING)
    private RaiseStopType protectType;
    @Column(precision = 38, scale = 18)
    private BigDecimal protectValue;
    @Column(precision = 38, scale = 18)
    private BigDecimal protectedAmount;
    @Column(precision = 38, scale = 18)
    private BigDecimal protectedMarginPercent;

    @Enumerated(EnumType.STRING)
    private ManagedStopUpdateReason reason;

    protected ManagedStopHistoryEntity() {
    }

    public static ManagedStopHistoryEntity create(
            ManagedPositionEntity position,
            BigDecimal currentPrice,
            BigDecimal previousStopPrice,
            BigDecimal newStopPrice,
            BigDecimal unrealizedPnl,
            BigDecimal pnlPercent,
            BigDecimal priceChangePercent,
            ManagedRaiseStopCalculator.RaiseStopPlan plan,
            ManagedStopUpdateReason reason
    ) {
        ManagedStopHistoryEntity entity = new ManagedStopHistoryEntity();
        entity.managedPosition = position;
        entity.userId = position.getUserId();
        entity.symbol = position.getSymbol();
        entity.side = position.getEntrySide();
        entity.changedAt = LocalDateTime.now();
        entity.currentPrice = currentPrice;
        entity.previousStopPrice = previousStopPrice;
        entity.newStopPrice = newStopPrice;
        entity.triggerBasis = position.getStopTriggerBasis();
        entity.triggerValue = position.getRaiseTriggerValue();
        entity.triggerPrice = plan == null ? null : plan.triggerPrice();
        entity.unrealizedPnl = unrealizedPnl;
        entity.pnlPercent = pnlPercent;
        entity.priceChangePercent = priceChangePercent;
        entity.protectType = position.getRaiseStopType();
        entity.protectValue = position.getRaiseStopValue();
        entity.protectedAmount = plan == null ? null : plan.protectedAmount();
        entity.protectedMarginPercent = plan == null ? null : plan.protectedPercent();
        entity.reason = reason;
        return entity;
    }

    public Long getId() {
        return id;
    }

    public ManagedPositionEntity getManagedPosition() {
        return managedPosition;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public BigDecimal getPreviousStopPrice() {
        return previousStopPrice;
    }

    public BigDecimal getNewStopPrice() {
        return newStopPrice;
    }

    public TriggerBasis getTriggerBasis() {
        return triggerBasis;
    }

    public BigDecimal getTriggerValue() {
        return triggerValue;
    }

    public BigDecimal getTriggerPrice() {
        return triggerPrice;
    }

    public BigDecimal getUnrealizedPnl() {
        return unrealizedPnl;
    }

    public BigDecimal getPnlPercent() {
        return pnlPercent;
    }

    public BigDecimal getPriceChangePercent() {
        return priceChangePercent;
    }

    public RaiseStopType getProtectType() {
        return protectType;
    }

    public BigDecimal getProtectValue() {
        return protectValue;
    }

    public BigDecimal getProtectedAmount() {
        return protectedAmount;
    }

    public BigDecimal getProtectedMarginPercent() {
        return protectedMarginPercent;
    }

    public ManagedStopUpdateReason getReason() {
        return reason;
    }
}
