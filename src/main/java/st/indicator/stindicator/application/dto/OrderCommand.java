package st.indicator.stindicator.application.dto;

public class OrderCommand {
    // 거래소 주문 대상 심볼
    String symbol;
    // BUY 또는 SELL 주문 방향
    String side;
    // MARKET 또는 LIMIT 주문 타입
    String type;
    // 지정가 주문의 시간 정책
    String timeInForce;
    // 거래소에 보낼 주문 수량
    String quantity;
    // 지정가 주문 가격
    String price;

    public OrderCommand(String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
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
