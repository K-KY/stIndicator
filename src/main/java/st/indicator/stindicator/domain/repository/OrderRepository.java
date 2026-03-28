package st.indicator.stindicator.domain.repository;

import org.springframework.data.domain.Pageable;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.infra.connector.entity.OrderEntity;

import java.util.List;

public interface OrderRepository {
    OrderEntity saveOrder(Order order);
    List<OrderEntity> getOrders(Pageable pageable);//체결되지 않은 주문
    OrderEntity getOrder(String orderId);//데이터 상세
    void cancelOrder(String orderId);//주문취소
}
