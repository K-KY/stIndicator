package st.indicator.stindicator.domain.repository;

import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;

import java.util.List;
import java.util.Optional;

public interface PositionMonitorRepository {
    PositionMonitor save(PositionMonitor positionMonitor);

    Optional<PositionMonitor> findById(Long id);

    List<PositionMonitor> findAllByStatus(PositionMonitorStatus status);

    List<PositionMonitor> findAllByUserId(Long userId);
}
