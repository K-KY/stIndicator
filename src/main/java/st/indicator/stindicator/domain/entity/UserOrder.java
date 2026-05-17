package st.indicator.stindicator.domain.entity;

public class UserOrder {
    // 우리 서비스가 추적하는 거래소 주문 ID
    private String orderId;
    // 사용자가 주문한 심볼
    private String symbol;
    // 주문 방향
    private String side;
    // 주문 타입
    private String type;
    // 주문 유지 정책
    private String timeInForce;
    // 사용자가 저장한 주문 수량
    private String quantity;
    // 사용자가 저장한 주문 가격
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
