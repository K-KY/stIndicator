package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import st.indicator.stindicator.presentation.dto.ChartIndicatorSettingRequestDto;
import st.indicator.stindicator.presentation.dto.ChartIndicatorSettingResponseDto;
import st.indicator.stindicator.presentation.dto.ChartRequestDto;
import st.indicator.stindicator.presentation.dto.ChartResponseDto;

@Tag(name = "Chart", description = "cursor 기반 캔들 및 기술 지표 API")
@RequestMapping("/api/v1/chart")
public interface ChartApi {
    @GetMapping
    @Operation(
            summary = "차트 구간 조회",
            description = "최신 또는 endTime 이전 캔들을 조회하고 요청한 SMA, EMA, RSI, MACD만 즉시 계산합니다."
    )
    ChartResponseDto chart(@ParameterObject ChartRequestDto request);

    @GetMapping("/settings")
    @Operation(
            summary = "내 차트 지표 설정 조회",
            description = "세션 사용자 기준으로 저장된 EMA/SMA/볼린저 밴드/VWAP 차트 지표 설정을 조회합니다."
    )
    ChartIndicatorSettingResponseDto indicatorSettings(HttpSession session);

    @PutMapping("/settings")
    @Operation(
            summary = "내 차트 지표 설정 저장",
            description = "세션 사용자 기준으로 차트 지표 설정 JSON을 저장합니다. 저장된 설정은 다른 브라우저에서도 같은 계정으로 복구됩니다."
    )
    ChartIndicatorSettingResponseDto saveIndicatorSettings(@RequestBody ChartIndicatorSettingRequestDto request,
                                                           HttpSession session);
}
