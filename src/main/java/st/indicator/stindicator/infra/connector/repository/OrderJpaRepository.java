package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.infra.connector.entity.UserOrderEntity;

import java.util.List;

@Repository
public interface OrderJpaRepository extends JpaRepository<UserOrderEntity, Long> {
    List<UserOrder> findAllBySymbol(String symbol);
}
