package st.indicator.stindicator.presentation.ws.publisher;

import java.util.Set;

public record AccountPositionUpdateEvent(
        Set<String> symbols,
        String reason,
        long eventTime,
        long transactionTime
) {
    public boolean hasSymbols() {
        return symbols != null && !symbols.isEmpty();
    }
}
