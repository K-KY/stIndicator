package st.indicator.stindicator.presentation.dto;

import java.util.List;

/** 한 지표의 판정 설정 버전과 시간순 상태·이벤트 묶음이다. */
public record IndicatorSignalSeriesDto(
        long signalConfigVersion,
        List<IndicatorSignalStateDto> states,
        List<IndicatorSignalEventDto> events
) {
    public IndicatorSignalSeriesDto {
        states = states == null ? List.of() : List.copyOf(states);
        events = events == null ? List.of() : List.copyOf(events);
    }
}
