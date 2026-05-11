package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public UserOrder save(String orderId, OrderCommand dto) {
        UserOrder userOrder = new UserOrder(orderId, dto.getSymbol(), dto.getSide(),
                        dto.getType(), dto.getTimeInForce(),
                        dto.getQuantity(), dto.getPrice());
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }

    public UserOrder save(String orderId, String symbol, String side, String type,
                          String timeInForce, String quantity, String price) {
        UserOrder userOrder = new UserOrder(orderId, symbol, side, type, timeInForce, quantity, price);
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }

    public List<UserOrder> getOrders(String symbol) {
        return orderRepository.getOrders(symbol);
    }
}
