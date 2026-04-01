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
        return orderRepository.findAllBySymbol(symbol);
    }

    //List<Order>getOrder -> 주문 목록 보기
    //Orger getOrder  -> 주문 상세 보기 -> 거래소 api 호출 필요
    @Override
    public void cancelOrder(String orderId) {

    }
}
