package st.indicator.stindicator.presentation.dto;

import java.math.BigDecimal;

/** 차트 시그널 판정에만 사용하는 설정이다. 색상과 마커 같은 표현 설정은 포함하지 않는다. */
public record ChartSignalConfigDto(
        Vwap vwap,
        Macd macd,
        Rsi rsi,
        AdxDmi adxDmi
) {
    public record Vwap(
            Boolean enabled,
            Boolean confirmedCandleOnly,
            Boolean showProvisionalSignal,
            BigDecimal neutralDistanceBps,
            Integer slopeLookback,
            BigDecimal minimumSlopeBps,
            Integer minimumSessionBars,
            BigDecimal strongDistanceBps
    ) {
    }

    public record Macd(
            Boolean enabled,
            Boolean confirmedCandleOnly,
            Boolean showProvisionalSignal,
            Boolean requireZeroLineConfirmation,
            BigDecimal minimumHistogramBps,
            Integer crossConfirmationBars,
            Boolean strongRequiresZeroLine
    ) {
    }

    public record Rsi(
            Boolean enabled,
            Boolean confirmedCandleOnly,
            Boolean showProvisionalSignal,
            BigDecimal neutralLower,
            BigDecimal neutralUpper,
            BigDecimal strongLongLevel,
            BigDecimal strongShortLevel,
            Integer slopeLookback,
            BigDecimal minimumSlope,
            BigDecimal oversoldLevel,
            BigDecimal overboughtLevel,
            BigDecimal middleLevel
    ) {
    }

    public record AdxDmi(
            Boolean enabled,
            Boolean confirmedCandleOnly,
            Boolean showProvisionalSignal,
            BigDecimal weakTrendThreshold,
            BigDecimal strongTrendThreshold,
            BigDecimal minimumDiSpread
    ) {
    }
}
