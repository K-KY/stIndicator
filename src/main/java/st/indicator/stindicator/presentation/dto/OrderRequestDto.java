package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.OrderCommand;

public class OrderRequestDto {
    // 주문을 넣을 거래 심볼. Binance Futures 심볼 형식 예: BTCUSDT
    String symbol;
    // 주문 방향. BUY면 매수/롱 진입, SELL이면 매도/숏 진입 또는 청산
    String side;
    // 주문 유형. MARKET은 시장가, LIMIT은 지정가 주문에 사용
    String type;
    // LIMIT 주문의 유효 시간 정책. 예: GTC, IOC, FOK
    String timeInForce;
    // 실제로 거래소에 전송할 주문 수량
    String quantity;
    // LIMIT 주문에서 사용할 지정 가격. MARKET 주문에서는 보통 비워둔다
    String price;

    public OrderRequestDto(String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
    }

    public OrderRequestDto() {
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

    public OrderCommand toCommand() {
        return new OrderCommand(symbol, side, type, timeInForce, quantity, price);
    }
}
