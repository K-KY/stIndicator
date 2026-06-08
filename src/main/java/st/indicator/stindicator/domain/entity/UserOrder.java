package st.indicator.stindicator.domain.entity;

public class UserOrder {
    // 우리 서비스가 추적하는 거래소 주문 ID
    private String orderId;
    // 주문을 실행한 서비스 사용자 ID. 비로그인 주문은 null일 수 있다.
    private Long userId;
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
    // 거래소 주문 상태 또는 서비스 저장 상태
    private String status;
    // 서비스에 주문 이력이 저장된 시각
    private java.time.LocalDateTime createdAt;

    public UserOrder() {
    }

    public UserOrder(String orderId, String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this(orderId, null, symbol, side, type, timeInForce, quantity, price, "SAVED", null);
    }

    public UserOrder(String orderId, Long userId, String symbol, String side, String type,
                     String timeInForce, String quantity, String price) {
        this(orderId, userId, symbol, side, type, timeInForce, quantity, price, "SAVED", null);
    }

    public UserOrder(String orderId, Long userId, String symbol, String side, String type,
                     String timeInForce, String quantity, String price, String status,
                     java.time.LocalDateTime createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public Long getUserId() {
        return userId;
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

    public String getStatus() {
        return status;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
