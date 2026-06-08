package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import st.indicator.stindicator.domain.entity.UserOrder;

import java.time.LocalDateTime;

@Entity
public class UserOrderEntity {

    @Id
    String orderId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    UserEntity user;

    String symbol;
    String side;
    String type;
    String timeInForce;
    String quantity;
    String price;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    public UserOrderEntity() {
    }

    public UserOrderEntity(String orderId, String symbol, String side, String type, String timeInForce, String quantity, String price) {
        this(orderId, null, symbol, side, type, timeInForce, quantity, price);
    }

    public UserOrderEntity(String orderId, Long userId, String symbol, String side, String type,
                           String timeInForce, String quantity, String price) {
        this(orderId, userId, symbol, side, type, timeInForce, quantity, price, "SAVED", null);
    }

    public UserOrderEntity(String orderId, Long userId, String symbol, String side, String type,
                           String timeInForce, String quantity, String price, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.user = userId == null ? null : UserEntity.reference(userId);
        this.symbol = symbol;
        this.side = side;
        this.type = type;
        this.timeInForce = timeInForce;
        this.quantity = quantity;
        this.price = price;
        this.status = status == null || status.isBlank() ? "SAVED" : status;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getOrderId() {
        return orderId;
    }

    public UserEntity getUser() {
        return user;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void updateStatus(String status) {
        this.status = status;
    }

    public static UserOrderEntity from(UserOrder userOrder) {
        return new UserOrderEntity(userOrder.getOrderId(), userOrder.getUserId(), userOrder.getSymbol(),
                userOrder.getSide(), userOrder.getType(), userOrder.getTimeInForce(),
                userOrder.getQuantity(), userOrder.getPrice(), userOrder.getStatus(), userOrder.getCreatedAt());
    }

    public UserOrder toDomain() {
        Long userId = user == null ? null : user.getId();
        return new UserOrder(orderId, userId, symbol, side, type, timeInForce, quantity, price,
                status == null ? "SAVED" : status, createdAt);
    }
}
