package st.indicator.stindicator.presentation.dto;

import java.util.List;

/** 특정 캔들에서 한 번 발생한 지표 시그널 이벤트 응답이다. */
public record IndicatorSignalEventDto(
        long time,
        long originTime,
        SignalEventType type,
        SignalDirection direction,
        boolean confirmed,
        List<SignalReasonCode> reasons
) {
    public IndicatorSignalEventDto {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}
