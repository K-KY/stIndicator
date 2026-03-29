package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.OrderCommand;

public class OrderRequestDto {
    String symbol;//이름
    String side;//buy, sell
    String type;//limit, market 지정가, 시장가
    String timeInForce;//gtc, gtd 취소시까지, 그 날까지만
    String quantity;//개수
    String price;//가격

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
