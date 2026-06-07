package st.indicator.stindicator.presentation.ws.subscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;
import st.indicator.stindicator.presentation.ws.publisher.MonitorEventPublisher;
import st.indicator.stindicator.presentation.ws.publisher.StopTriggerEvent;
import st.indicator.stindicator.presentation.ws.service.PositionMonitorService;
import st.indicator.stindicator.application.service.OrderService;

@Component
public class StopTriggerSubscriber {
    private static final Logger log = LoggerFactory.getLogger(StopTriggerSubscriber.class);
    private final OrderService orderService;
    private final PositionMonitorService positionMonitorService;
    private final MonitorEventPublisher monitorEventPublisher;

    public StopTriggerSubscriber(OrderService orderService,
                                 PositionMonitorService positionMonitorService,
                                 MonitorEventPublisher monitorEventPublisher) {
        this.orderService = orderService;
        this.positionMonitorService = positionMonitorService;
        this.monitorEventPublisher = monitorEventPublisher;
    }

    @EventListener
    public void onStopTriggered(StopTriggerEvent event) {
        PositionMonitor positionMonitor = event.positionMonitor();
        try {
            Order order = orderService.closeMonitoredPosition(positionMonitor);
            PositionMonitor closed = positionMonitorService.updateAfterClose(positionMonitor, PositionMonitorStatus.CLOSED);
            positionMonitorService.markClosed(closed);
            monitorEventPublisher.publishOrderExecuted(closed, order);
        } catch (Exception e) {
            log.error("monitor close order failed monitorId={}", positionMonitor.getId(), e);
            PositionMonitor failed = positionMonitorService.updateAfterClose(positionMonitor, PositionMonitorStatus.FAILED);
            positionMonitorService.markClosed(failed);
        }
    }
}
