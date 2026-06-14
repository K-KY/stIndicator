package st.indicator.stindicator.presentation.controller;

import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.ChartService;
import st.indicator.stindicator.presentation.dto.ChartRequestDto;
import st.indicator.stindicator.presentation.dto.ChartResponseDto;

@RestController
public class ChartController implements ChartApi {
    private final ChartService chartService;

    public ChartController(ChartService chartService) {
        this.chartService = chartService;
    }

    @Override
    public ChartResponseDto chart(ChartRequestDto request) {
        return chartService.getChart(request);
    }
}
