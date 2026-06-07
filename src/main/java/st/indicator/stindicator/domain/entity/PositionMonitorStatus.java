package st.indicator.stindicator.domain.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "포지션 모니터링 상태")
public enum PositionMonitorStatus {
    ACTIVE,
    STOPPED,
    CLOSED,
    FAILED
}
