package st.indicator.stindicator.presentation.ws.publisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionMonitor;

import java.math.BigDecimal;

@Component
public class MonitorEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;

    public MonitorEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    //
    public void publishPriceTick(String symbol, BigDecimal price, long eventTime) {
        applicationEventPublisher.publishEvent(new PriceTickEvent(symbol, price, eventTime));
    }

    public void publishStopTriggered(PositionMonitor positionMonitor) {
        applicationEventPublisher.publishEvent(new StopTriggerEvent(positionMonitor));
    }

    public void publishOrderExecuted(PositionMonitor positionMonitor, Order order) {
        applicationEventPublisher.publishEvent(new OrderExecutedEvent(positionMonitor, order));
    }
}
