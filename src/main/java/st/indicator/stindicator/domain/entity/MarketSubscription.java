package st.indicator.stindicator.domain.entity;

import java.time.LocalDateTime;

/**
 * 사용자가 홈 화면에서 실시간 시장 가격을 보기 위해 저장한 심볼 구독 상태다.
 */
public class MarketSubscription {
    private final Long id;
    private final Long userId;
    private final String symbol;
    private final String interval;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public MarketSubscription(Long id, Long userId, String symbol, String interval,
                              LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.interval = interval;
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

    public String getInterval() {
        return interval;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
