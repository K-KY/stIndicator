package st.indicator.stindicator.domain.repository;

import st.indicator.stindicator.domain.entity.UserOrder;

public interface OrderRepository {
    void saveOrder(UserOrder userOrder);
    void cancelOrder(String orderId);//주문취소
}
