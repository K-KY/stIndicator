package st.indicator.stindicator.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모니터링 청산 주문 타입")
public enum MonitorOrderType {
    MARKET,
    LIMIT
}
