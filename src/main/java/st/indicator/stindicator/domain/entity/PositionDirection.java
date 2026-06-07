package st.indicator.stindicator.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포지션 또는 주문 방향")
public enum PositionDirection {
    BUY,
    SELL
}
