package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.ChartIndicatorSettingEntity;

import java.util.Optional;

public interface ChartIndicatorSettingJpaRepository extends JpaRepository<ChartIndicatorSettingEntity, Long> {
    Optional<ChartIndicatorSettingEntity> findByUser_Id(Long userId);
}
