package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import st.indicator.stindicator.domain.entity.MarketSubscription;

import java.time.LocalDateTime;

@Entity
@Table(name = "market_subscription", uniqueConstraints = {
        @UniqueConstraint(name = "uk_market_subscription_user_symbol", columnNames = {"user_id", "symbol"})
})
public class MarketSubscriptionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Column(nullable = false, length = 10)
    private String intervalValue;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public MarketSubscriptionEntity() {
    }

    public MarketSubscriptionEntity(Long id, UserEntity user, String symbol, String intervalValue,
                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.user = user;
        this.symbol = symbol;
        this.intervalValue = intervalValue;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MarketSubscriptionEntity create(Long userId, String symbol, String interval) {
        return new MarketSubscriptionEntity(null, UserEntity.reference(userId), symbol.toUpperCase(),
                normalizeInterval(interval), null, null);
    }

    public void updateInterval(String interval) {
        this.intervalValue = normalizeInterval(interval);
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

    public MarketSubscription toDomain() {
        return new MarketSubscription(id, user.getId(), symbol, intervalValue, createdAt, updatedAt);
    }

    private static String normalizeInterval(String interval) {
        return interval == null || interval.isBlank() ? "1m" : interval.toLowerCase();
    }
}
