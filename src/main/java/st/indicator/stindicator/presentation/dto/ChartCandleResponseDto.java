package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.utils.candle.Candle;

import java.math.BigDecimal;

public record ChartCandleResponseDto(
        Long openTime,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close,
        BigDecimal volume,
        Long closeTime
) {
    public static ChartCandleResponseDto from(Candle candle) {
        return new ChartCandleResponseDto(
                candle.getOpenTime(),
                candle.getOpen(),
                candle.getHigh(),
                candle.getLow(),
                candle.getClose(),
                candle.getVolume(),
                candle.getCloseTime()
        );
    }
}
