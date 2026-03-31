package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;

@Service
public class OrderService {
    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public UserOrder save(String orderId, OrderCommand dto) {
        UserOrder userOrder = new UserOrder(orderId, dto.getSymbol(), dto.getSide(),
                        dto.getType(), dto.getTimeInForce(),
                        dto.getPrice(), dto.getQuantity());
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }
}
