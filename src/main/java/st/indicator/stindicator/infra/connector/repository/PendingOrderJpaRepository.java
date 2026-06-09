package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.domain.entity.PendingOrderStatus;
import st.indicator.stindicator.infra.connector.entity.PendingOrderEntity;

import java.util.List;
import java.util.Optional;

public interface PendingOrderJpaRepository extends JpaRepository<PendingOrderEntity, Long> {
    Optional<PendingOrderEntity> findFirstByOrderId(String orderId);
    List<PendingOrderEntity> findAllByStatusOrderByCreatedAtDesc(PendingOrderStatus status);
}
