package st.indicator.stindicator.presentation.dto;

import java.util.List;

/** 한 캔들에서 계산된 지표 시그널의 지속 상태 응답이다. */
public record IndicatorSignalStateDto(
        long time,
        SignalDirection direction,
        SignalStrength strength,
        boolean confirmed,
        List<SignalReasonCode> reasons
) {
    public IndicatorSignalStateDto {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
        if (direction == SignalDirection.NEUTRAL && strength != SignalStrength.NONE) {
            throw new IllegalArgumentException("NEUTRAL signal strength must be NONE");
        }
    }
}
