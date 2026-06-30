package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.indicator.stindicator.infra.connector.entity.ChartIndicatorSettingEntity;
import st.indicator.stindicator.infra.connector.repository.ChartIndicatorSettingJpaRepository;

import java.util.Optional;

@Service
public class ChartIndicatorSettingService {
    private static final int MAX_SETTINGS_JSON_LENGTH = 20_000;

    private final ChartIndicatorSettingJpaRepository chartIndicatorSettingJpaRepository;

    public ChartIndicatorSettingService(ChartIndicatorSettingJpaRepository chartIndicatorSettingJpaRepository) {
        this.chartIndicatorSettingJpaRepository = chartIndicatorSettingJpaRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ChartIndicatorSettingEntity> find(Long userId) {
        return chartIndicatorSettingJpaRepository.findByUser_Id(userId);
    }

    @Transactional
    public ChartIndicatorSettingEntity save(Long userId, String settingsJson) {
        validate(settingsJson);
        ChartIndicatorSettingEntity entity = chartIndicatorSettingJpaRepository.findByUser_Id(userId)
                .orElseGet(() -> ChartIndicatorSettingEntity.create(userId, settingsJson));
        entity.updateSettings(settingsJson);
        return chartIndicatorSettingJpaRepository.save(entity);
    }

    private void validate(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            throw new IllegalArgumentException("차트 지표 설정은 비어 있을 수 없습니다.");
        }
        if (settingsJson.length() > MAX_SETTINGS_JSON_LENGTH) {
            throw new IllegalArgumentException("차트 지표 설정 크기가 너무 큽니다.");
        }
    }
}
