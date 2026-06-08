package st.indicator.stindicator.infra.connector.repository;

import org.springframework.stereotype.Repository;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;
import st.indicator.stindicator.domain.repository.PositionMonitorRepository;
import st.indicator.stindicator.infra.connector.entity.PositionMonitorEntity;

import java.util.List;
import java.util.Optional;

@Repository
public class PositionMonitorRepositoryImpl implements PositionMonitorRepository {
    private final PositionMonitorJpaRepository positionMonitorJpaRepository;

    public PositionMonitorRepositoryImpl(PositionMonitorJpaRepository positionMonitorJpaRepository) {
        this.positionMonitorJpaRepository = positionMonitorJpaRepository;
    }

    @Override
    public PositionMonitor save(PositionMonitor positionMonitor) {
        return positionMonitorJpaRepository.save(PositionMonitorEntity.from(positionMonitor)).toDomain();
    }

    @Override
    public Optional<PositionMonitor> findById(Long id) {
        return positionMonitorJpaRepository.findById(id).map(PositionMonitorEntity::toDomain);
    }

    @Override
    public List<PositionMonitor> findAllByStatus(PositionMonitorStatus status) {
        return positionMonitorJpaRepository.findAllByStatus(status)
                .stream()
                .map(PositionMonitorEntity::toDomain)
                .toList();
    }

    @Override
    public List<PositionMonitor> findAllByUserId(Long userId) {
        return positionMonitorJpaRepository.findAllByUser_Id(userId)
                .stream()
                .map(PositionMonitorEntity::toDomain)
                .toList();
    }
}
