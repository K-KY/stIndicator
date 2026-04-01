package st.indicator.stindicator.domain.repository;

import st.indicator.stindicator.domain.entity.UserOrder;

import java.util.List;

public interface OrderRepository {
    void saveOrder(UserOrder userOrder);
    List<UserOrder> getOrders(String symbol);
    void cancelOrder(String orderId);//주문취소
}
