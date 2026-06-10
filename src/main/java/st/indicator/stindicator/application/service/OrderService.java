package st.indicator.stindicator.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

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

    public UserOrder save(String orderId, OrderCommand dto) {
        UserOrder userOrder = new UserOrder(normalizeOrderId(orderId), dto.getSymbol(), dto.getSide(),
                        dto.getType(), dto.getTimeInForce(),
                        dto.getQuantity(), dto.getPrice());
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }

    public UserOrder save(Long userId, String orderId, OrderCommand dto) {
        if (userId == null) {
            return save(orderId, dto);
        }
        UserOrder userOrder = new UserOrder(normalizeOrderId(orderId), userId, dto.getSymbol(), dto.getSide(),
                dto.getType(), dto.getTimeInForce(), dto.getQuantity(), dto.getPrice());
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }

    public UserOrder save(String orderId, String symbol, String side, String type,
                          String timeInForce, String quantity, String price) {
        return save(orderId, symbol, side, type, timeInForce, quantity, price, "SAVED");
    }

    public UserOrder save(String orderId, String symbol, String side, String type,
                          String timeInForce, String quantity, String price, String status) {
        UserOrder userOrder = new UserOrder(normalizeOrderId(orderId), null, symbol, side, type,
                timeInForce, quantity, price, status, null);
        orderRepository.saveOrder(userOrder);
        return userOrder;
    }

    public UserOrder save(Long userId, String orderId, String symbol, String side, String type,
                          String timeInForce, String quantity, String price) {
        return save(userId, orderId, symbol, side, type, timeInForce, quantity, price, "SAVED");
    }

    public UserOrder save(Long userId, String orderId, String symbol, String side, String type,
                          String timeInForce, String quantity, String price, String status) {
        if (userId == null) {
            return save(orderId, symbol, side, type, timeInForce, quantity, price, status);
        }
        UserOrder userOrder = new UserOrder(normalizeOrderId(orderId), userId, symbol, side, type,
                timeInForce, quantity, price, status, null);
        orderRepository.saveOrder(userOrder);
        return userOrder;
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

    private String stringifyPrice(Order order) {
        return order.getPrice() == null ? null : order.getPrice().toPlainString();
    }

    private String normalizeOrderId(String orderId) {
        if (orderId != null && !orderId.isBlank()) {
            return orderId;
        }
        return "LOCAL-" + UUID.randomUUID();
    }
}
