package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.PositionRisk;

import java.math.BigDecimal;

public record UnmanagedPositionResponseDto(
        String symbol,
        String side,
        BigDecimal quantity,
        BigDecimal entryPrice,
        BigDecimal currentPrice,
        BigDecimal markPrice,
        BigDecimal unrealizedPnl,
        BigDecimal leverage,
        BigDecimal liquidationPrice,
        BigDecimal notional
) {
    public static UnmanagedPositionResponseDto from(PositionRisk position) {
        boolean longSide = position.getPositionAmt().signum() > 0;
        return new UnmanagedPositionResponseDto(
                position.getSymbol(),
                longSide ? "LONG" : "SHORT",
                position.getPositionAmt().abs(),
                position.getEntryPrice(),
                position.getMarkPrice(),
                position.getMarkPrice(),
                  position.getUnrealizedProfit(),
                  position.getLeverage(),
                  position.getLiquidationPrice(),
                  position.getNotional()
           );
      }
}
