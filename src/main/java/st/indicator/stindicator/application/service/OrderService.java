package st.indicator.stindicator.application.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.domain.entity.MonitorOrderType;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionDirection;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

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

    public Order placeMarketOrder(Long userId, String symbol, PositionDirection side, BigDecimal quantity) {
        Order order = requireClientService().order(new OrderCommand(
                symbol,
                side.name(),
                "MARKET",
                null,
                quantity.toPlainString(),
                null
        ));
        save(order.getOrderId(), order.getSymbol(), order.getSide(), order.getType(),
                order.getTimeInForce(), order.getOrigQty().toPlainString(), stringifyPrice(order));
        return order;
    }

    public Order placeLimitOrder(Long userId, String symbol, PositionDirection side, BigDecimal quantity,
                                 BigDecimal price, String timeInForce) {
        Order order = requireClientService().order(new OrderCommand(
                symbol,
                side.name(),
                "LIMIT",
                timeInForce == null ? "GTC" : timeInForce.toUpperCase(Locale.ROOT),
                quantity.toPlainString(),
                price.toPlainString()
        ));
        save(order.getOrderId(), order.getSymbol(), order.getSide(), order.getType(),
                order.getTimeInForce(), order.getOrigQty().toPlainString(), stringifyPrice(order));
        return order;
    }

    public Order closeMonitoredPosition(PositionMonitor positionMonitor) {
        PositionDirection closeSide = positionMonitor.getDirection() == PositionDirection.BUY
                ? PositionDirection.SELL
                : PositionDirection.BUY;

        String type = positionMonitor.getCloseOrderType() == MonitorOrderType.LIMIT ? "LIMIT" : "MARKET";
        String timeInForce = "LIMIT".equals(type) ? "GTC" : null;
        String price = positionMonitor.getCloseOrderType() == MonitorOrderType.LIMIT && positionMonitor.getCloseLimitPrice() != null
                ? positionMonitor.getCloseLimitPrice().toPlainString()
                : null;

        Order order = requireClientService().order(new OrderCommand(
                positionMonitor.getSymbol(),
                closeSide.name(),
                type,
                timeInForce,
                positionMonitor.getQuantity().abs().toPlainString(),
                price
        ));
        save(order.getOrderId(), order.getSymbol(), order.getSide(), order.getType(),
                order.getTimeInForce(), order.getOrigQty().toPlainString(), stringifyPrice(order));
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
}
