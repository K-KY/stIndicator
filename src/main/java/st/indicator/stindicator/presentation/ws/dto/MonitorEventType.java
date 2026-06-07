package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클라이언트 WebSocket 이벤트 타입")
public enum MonitorEventType {
    PRICE_UPDATE,
    POSITION_UPDATE,
    TRAILING_STOP_UPDATED,
    STOP_TRIGGERED,
    ORDER_EXECUTED
}
