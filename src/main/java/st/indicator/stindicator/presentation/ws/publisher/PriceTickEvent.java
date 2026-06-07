package st.indicator.stindicator.presentation.ws.publisher;

import java.math.BigDecimal;

public record PriceTickEvent(String symbol, BigDecimal price, long eventTime) {
}
