package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import st.indicator.stindicator.domain.entity.UserOrder;

@Entity
public class UserOrderEntity {

    @Id
    String orderId;

    String symbol;
    String side;
    String type;
    String timeInForce;
    String quantity;
    String price;

    public UserOrderEntity() {
    }

    public UserOrderEntity(String orderId, String symbol, String side, String type, String timeInForce, String quantity, String price) {
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

    public static UserOrderEntity from(UserOrder userOrder) {
        return new UserOrderEntity(userOrder.getOrderId(), userOrder.getSymbol(), userOrder.getSide(), userOrder.getType(),
                userOrder.getTimeInForce(), userOrder.getQuantity(), userOrder.getPrice());
    }
}
