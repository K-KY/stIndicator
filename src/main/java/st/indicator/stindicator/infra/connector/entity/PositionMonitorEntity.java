package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import st.indicator.stindicator.domain.entity.MonitorOrderType;
import st.indicator.stindicator.domain.entity.PositionDirection;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "position_monitor")
public class PositionMonitorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PositionDirection direction;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal entryPrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal leverage;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal currentProfitPercent;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal highestProfitPercent;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal trailingStopPercent;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal currentStopLine;

    @Column(nullable = false, precision = 30, scale = 8)
    private BigDecimal trailingGapPercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MonitorOrderType closeOrderType;

    @Column(precision = 30, scale = 8)
    private BigDecimal closeLimitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PositionMonitorStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public PositionMonitorEntity() {
    }

    public PositionMonitorEntity(Long id, Long userId, String symbol, PositionDirection direction,
                                 BigDecimal entryPrice, BigDecimal currentPrice, BigDecimal quantity,
                                 BigDecimal leverage, BigDecimal currentProfitPercent,
                                 BigDecimal highestProfitPercent, BigDecimal trailingStopPercent,
                                 BigDecimal currentStopLine, BigDecimal trailingGapPercent,
                                 MonitorOrderType closeOrderType, BigDecimal closeLimitPrice,
                                 PositionMonitorStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = UserEntity.reference(userId);
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

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static PositionMonitorEntity from(PositionMonitor positionMonitor) {
        return new PositionMonitorEntity(
                positionMonitor.getId(),
                positionMonitor.getUserId(),
                positionMonitor.getSymbol(),
                positionMonitor.getDirection(),
                positionMonitor.getEntryPrice(),
                positionMonitor.getCurrentPrice(),
                positionMonitor.getQuantity(),
                positionMonitor.getLeverage(),
                positionMonitor.getCurrentProfitPercent(),
                positionMonitor.getHighestProfitPercent(),
                positionMonitor.getTrailingStopPercent(),
                positionMonitor.getCurrentStopLine(),
                positionMonitor.getTrailingGapPercent(),
                positionMonitor.getCloseOrderType(),
                positionMonitor.getCloseLimitPrice(),
                positionMonitor.getStatus(),
                positionMonitor.getCreatedAt(),
                positionMonitor.getUpdatedAt()
        );
    }

    public PositionMonitor toDomain() {
        return new PositionMonitor(
                id,
                user.getId(),
                symbol,
                direction,
                entryPrice,
                currentPrice,
                quantity,
                leverage,
                currentProfitPercent,
                highestProfitPercent,
                trailingStopPercent,
                currentStopLine,
                trailingGapPercent,
                closeOrderType,
                closeLimitPrice,
                status,
                createdAt,
                updatedAt
        );
    }
}
