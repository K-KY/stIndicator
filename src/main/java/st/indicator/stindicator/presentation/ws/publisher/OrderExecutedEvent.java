package st.indicator.stindicator.presentation.ws.publisher;

import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionMonitor;

public record OrderExecutedEvent(PositionMonitor positionMonitor, Order order) {
}
