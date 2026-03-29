package st.indicator.stindicator.application.dto;

public class OrderCommand {
    String symbol;
    String side;
    String type;
    String timeInForce;
    String quantity;
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
