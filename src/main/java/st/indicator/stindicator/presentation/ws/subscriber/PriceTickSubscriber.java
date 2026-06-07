package st.indicator.stindicator.presentation.ws.subscriber;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.presentation.ws.publisher.PriceTickEvent;
import st.indicator.stindicator.presentation.ws.service.PositionMonitorService;

@Component
public class PriceTickSubscriber {
    private final PositionMonitorService positionMonitorService;

    public PriceTickSubscriber(PositionMonitorService positionMonitorService) {
        this.positionMonitorService = positionMonitorService;
    }

    @EventListener
    public void onPriceTick(PriceTickEvent event) {
        positionMonitorService.handlePriceTick(event.symbol(), event.price(), event.eventTime());
    }
}
