package st.indicator.stindicator.presentation.ws.subscriber;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.presentation.ws.dto.MonitorEventType;
import st.indicator.stindicator.presentation.ws.dto.MonitorSocketEventDto;
import st.indicator.stindicator.presentation.ws.dto.OrderExecutionResponseDto;
import st.indicator.stindicator.presentation.ws.publisher.OrderExecutedEvent;

@Component
public class OrderExecutedSubscriber {
    private final MonitorService monitorService;

    public OrderExecutedSubscriber(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @EventListener
    public void onOrderExecuted(OrderExecutedEvent event) {
        monitorService.publishMonitorEvent(event.positionMonitor().getSymbol(), new MonitorSocketEventDto(
                MonitorEventType.ORDER_EXECUTED,
                event.positionMonitor().getUserId(),
                event.positionMonitor().getSymbol(),
                event.positionMonitor().getId(),
                OrderExecutionResponseDto.from(event.order())
        ));
    }
}
