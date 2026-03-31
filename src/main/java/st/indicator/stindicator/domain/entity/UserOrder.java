package st.indicator.stindicator.domain.entity;

public class UserOrder {
    private String orderId;
    private String symbol;
    private String side;
    private String type;
    private String timeInForce;
    private String quantity;
    private String price;

    public UserOrder() {
    }

    public UserOrder(String orderId, String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getType() {
        return type;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getPrice() {
        return price;
    }
}
