package st.indicator.stindicator.infra.connector.repository;

import org.springframework.stereotype.Repository;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.domain.repository.OrderRepository;
import st.indicator.stindicator.infra.connector.entity.UserOrderEntity;

import java.util.List;

@Repository
public class OrderRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository orderRepository;

    public OrderRepositoryImpl(OrderJpaRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void saveOrder(UserOrder userOrder) {

        UserOrderEntity userOrderEntity = UserOrderEntity.from(userOrder);
        UserOrderEntity save = orderRepository.save(userOrderEntity);
    }

    @Override
    public List<UserOrder> getOrders(String symbol) {
        return orderRepository.findAllBySymbolOrderByCreatedAtDesc(symbol).stream()
                .map(UserOrderEntity::toDomain)
                .toList();
    }

    @Override
    public void cancelOrder(String orderId) {
        orderRepository.findById(orderId).ifPresent(entity -> {
            entity.updateStatus("CANCELED");
            orderRepository.save(entity);
        });
    }
}
