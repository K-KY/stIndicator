package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자별 차트 지표 설정 저장 요청")
public class ChartIndicatorSettingRequestDto {
    @Schema(description = "프론트 차트 지표 설정 JSON 문자열")
    private String settingsJson;

    public String getSettingsJson() {
        return settingsJson;
    }

    public void setSettingsJson(String settingsJson) {
        this.settingsJson = settingsJson;
    }
}
