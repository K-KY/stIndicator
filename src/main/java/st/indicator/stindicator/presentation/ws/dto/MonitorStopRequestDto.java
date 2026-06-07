package st.indicator.stindicator.presentation.ws.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모니터링 중지 요청 DTO")
public class MonitorStopRequestDto {
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    @Schema(description = "중지할 모니터링 ID", example = "10")
    private Long monitorId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }
}
