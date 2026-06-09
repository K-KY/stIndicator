package st.indicator.stindicator.presentation.ws.publisher;

import java.math.BigDecimal;

public record OrderTradeUpdateEvent(
        String symbol,
        String clientOrderId,
        String side,
        String orderType,
        String executionType,
        String orderStatus,
        String orderId,
        BigDecimal averagePrice,
        BigDecimal executedQuantity,
        long eventTime,
        long transactionTime
) {
    public boolean isFilled() {
        return "FILLED".equalsIgnoreCase(orderStatus);
    }

    public boolean isCanceled() {
        return "CANCELED".equalsIgnoreCase(orderStatus);
    }

    public boolean isExpired() {
        return "EXPIRED".equalsIgnoreCase(orderStatus) || "EXPIRED_IN_MATCH".equalsIgnoreCase(orderStatus);
    }
}
