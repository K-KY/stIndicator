package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "클라이언트 WebSocket으로 전달하는 모니터 이벤트 DTO")
public class MonitorSocketEventDto {
    @Schema(description = "이벤트 타입")
    private final MonitorEventType type;
    @Schema(description = "사용자 ID")
    private final Long userId;
    @Schema(description = "심볼")
    private final String symbol;
    @Schema(description = "모니터 ID")
    private final Long monitorId;
    @Schema(description = "이벤트 페이로드")
    private final Object payload;

    public MonitorSocketEventDto(MonitorEventType type, Long userId, String symbol, Long monitorId, Object payload) {
        this.type = type;
        this.userId = userId;
        this.symbol = symbol;
        this.monitorId = monitorId;
        this.payload = payload;
    }

    public MonitorEventType getType() {
        return type;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public Object getPayload() {
        return payload;
    }
}
