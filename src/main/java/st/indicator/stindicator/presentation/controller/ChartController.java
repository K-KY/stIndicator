package st.indicator.stindicator.presentation.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.ChartIndicatorSettingService;
import st.indicator.stindicator.application.service.ChartService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.presentation.dto.ChartIndicatorSettingRequestDto;
import st.indicator.stindicator.presentation.dto.ChartIndicatorSettingResponseDto;
import st.indicator.stindicator.presentation.dto.ChartRequestDto;
import st.indicator.stindicator.presentation.dto.ChartResponseDto;

@RestController
public class ChartController implements ChartApi {
    private static final Logger log = LoggerFactory.getLogger(ChartController.class);

    private final ChartService chartService;
    private final ChartIndicatorSettingService chartIndicatorSettingService;

    public ChartController(ChartService chartService, ChartIndicatorSettingService chartIndicatorSettingService) {
        this.chartService = chartService;
        this.chartIndicatorSettingService = chartIndicatorSettingService;
    }

    @Override
    public ChartResponseDto chart(ChartRequestDto request) {
        log.info("request chart symbol={}, interval={}, limit={}, before={}, after={}, indicators={}, emaPeriods={}, smaPeriods={}, bollingerPeriod={}, vwap={}",
                request.getSymbol(),
                request.getInterval(),
                request.getLimit(),
                request.getBefore(),
                request.getAfter(),
                request.getIndicators(),
                request.getEmaPeriods(),
                request.getSmaPeriods(),
                request.getBollingerPeriod(),
                request.getVwap());
        return chartService.getChart(request);
    }

    @Override
    public ChartIndicatorSettingResponseDto indicatorSettings(HttpSession session) {
        Long userId = sessionUserId(session);
        if (userId == null) {
            return ChartIndicatorSettingResponseDto.empty();
        }
        return chartIndicatorSettingService.find(userId)
                .map(ChartIndicatorSettingResponseDto::from)
                .orElseGet(ChartIndicatorSettingResponseDto::empty);
    }

    @Override
    public ChartIndicatorSettingResponseDto saveIndicatorSettings(ChartIndicatorSettingRequestDto request,
                                                                  HttpSession session) {
        Long userId = sessionUserId(session);
        if (userId == null) {
            throw new IllegalStateException("로그인이 필요합니다.");
        }
        return ChartIndicatorSettingResponseDto.from(
                chartIndicatorSettingService.save(userId, request.getSettingsJson())
        );
    }

    private Long sessionUserId(HttpSession session) {
        Object userId = session.getAttribute(SessionUser.USER_ID);
        return userId instanceof Long id ? id : null;
    }
}
