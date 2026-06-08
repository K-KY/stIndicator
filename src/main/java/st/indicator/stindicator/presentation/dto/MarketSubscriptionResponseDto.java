package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.domain.entity.MarketSubscription;

@Schema(description = "사용자 시장 데이터 구독 응답")
public class MarketSubscriptionResponseDto {
    @Schema(description = "구독 ID")
    private final Long id;

    @Schema(description = "사용자 ID")
    private final Long userId;

    @Schema(description = "구독 심볼", example = "BTCUSDT")
    private final String symbol;

    @Schema(description = "캔들 주기", example = "1m")
    private final String interval;

    public MarketSubscriptionResponseDto(Long id, Long userId, String symbol, String interval) {
        this.id = id;
        this.userId = userId;
        this.symbol = symbol;
        this.interval = interval;
    }

    public static MarketSubscriptionResponseDto from(MarketSubscription subscription) {
        return new MarketSubscriptionResponseDto(
                subscription.getId(),
                subscription.getUserId(),
                subscription.getSymbol(),
                subscription.getInterval()
        );
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
}
