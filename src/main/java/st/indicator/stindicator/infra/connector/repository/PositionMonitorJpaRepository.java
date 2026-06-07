package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;
import st.indicator.stindicator.infra.connector.entity.PositionMonitorEntity;

import java.util.List;

public interface PositionMonitorJpaRepository extends JpaRepository<PositionMonitorEntity, Long> {
    List<PositionMonitorEntity> findAllByStatus(PositionMonitorStatus status);

    List<PositionMonitorEntity> findAllByUserId(Long userId);
}
