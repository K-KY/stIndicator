package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.ManagedStopHistoryEntity;

import java.math.BigDecimal;
import java.util.List;

public interface ManagedStopHistoryJpaRepository extends JpaRepository<ManagedStopHistoryEntity, Long> {
    List<ManagedStopHistoryEntity> findAllByManagedPosition_IdAndUserIdOrderByChangedAtDesc(
            Long managedPositionId,
            Long userId
    );

    boolean existsByManagedPosition_IdAndNewStopPrice(Long managedPositionId, BigDecimal newStopPrice);
}
