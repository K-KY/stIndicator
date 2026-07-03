package st.indicator.stindicator.presentation.dto;

/** 원본 지표와 분리된 지표별 시그널 판정 응답이다. */
public record ChartIndicatorSignalsDto(
        IndicatorSignalSeriesDto vwap,
        IndicatorSignalSeriesDto macd,
        IndicatorSignalSeriesDto rsi,
        IndicatorSignalSeriesDto adxDmi
) {
    public boolean isEmpty() {
        return vwap == null && macd == null && rsi == null && adxDmi == null;
    }
}
