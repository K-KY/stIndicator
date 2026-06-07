package st.indicator.stindicator.presentation.ws.subscriber;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.presentation.ws.service.PositionMonitorService;

@Component
public class PositionMonitorRecoverySubscriber {
    private final PositionMonitorService positionMonitorService;

    public PositionMonitorRecoverySubscriber(PositionMonitorService positionMonitorService) {
        this.positionMonitorService = positionMonitorService;
    }

    //애플리케이션 시작시 호출될 이벤트 리스너
    @EventListener(ApplicationReadyEvent.class)
    public void restore() {
        positionMonitorService.restoreActiveMonitors();//기존에 구독중이던 심볼들 확인후 복원
    }
}
