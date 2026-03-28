package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.OrderEntity;

public interface OrderJpaRepository extends JpaRepository<OrderEntity, String> {
}
