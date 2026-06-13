package st.indicator.stindicator.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;

import java.util.List;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ClientService clientService;

    public OrderService(OrderRepository orderRepository) {
        this(orderRepository, null);//테스트 mocking용
    }

    @Autowired
    public OrderService(OrderRepository orderRepository, ClientService clientService) {
        this.orderRepository = orderRepository;
        this.clientService = clientService;
    }

    public List<UserOrder> getOrders(String symbol) {
        return orderRepository.getOrders(symbol);
    }

    public List<UserOrder> getOrders(Long userId, String symbol) {
        return orderRepository.getOrders(userId, symbol);
    }

    public Order cancelOrder(String symbol, String orderId) {
        Order order = requireClientService().cancelOrder(symbol, orderId);
        orderRepository.cancelOrder(orderId);
        return order;
    }

    private ClientService requireClientService() {
        if (clientService == null) {
            throw new IllegalStateException("ClientService is required for order execution");
        }
        return clientService;
    }

}
