package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.infra.connector.entity.ChartIndicatorSettingEntity;

import java.time.LocalDateTime;

@Schema(description = "사용자별 차트 지표 설정 응답")
public class ChartIndicatorSettingResponseDto {
    @Schema(description = "저장된 설정 존재 여부")
    private boolean exists;
    @Schema(description = "프론트 차트 지표 설정 JSON 문자열")
    private String settingsJson;
    @Schema(description = "마지막 저장 시각")
    private LocalDateTime updatedAt;

    public ChartIndicatorSettingResponseDto(boolean exists, String settingsJson, LocalDateTime updatedAt) {
        this.exists = exists;
        this.settingsJson = settingsJson;
        this.updatedAt = updatedAt;
    }

    public static ChartIndicatorSettingResponseDto empty() {
        return new ChartIndicatorSettingResponseDto(false, null, null);
    }

    public static ChartIndicatorSettingResponseDto from(ChartIndicatorSettingEntity entity) {
        return new ChartIndicatorSettingResponseDto(true, entity.getSettingsJson(), entity.getUpdatedAt());
    }

    public boolean isExists() {
        return exists;
    }

    public String getSettingsJson() {
        return settingsJson;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
