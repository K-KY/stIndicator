package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
