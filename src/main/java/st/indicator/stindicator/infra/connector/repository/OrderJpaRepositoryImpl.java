package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.repository.OrderRepository;
import st.indicator.stindicator.infra.connector.entity.OrderEntity;

import java.util.List;

@Repository
public class OrderJpaRepositoryImpl implements OrderRepository {
    private final OrderJpaRepository jpaRepository;

    public OrderJpaRepositoryImpl(OrderJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrderEntity saveOrder(Order order) {
        return jpaRepository.save(new OrderEntity(order));
    }

    @Override
    public List<OrderEntity> getOrders(Pageable pageable) {
        return jpaRepository.findAll(pageable).toList();
    }

    @Override
    public OrderEntity getOrder(String orderId) {
        return jpaRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Override
    public void cancelOrder(String orderId) {

    }
}
