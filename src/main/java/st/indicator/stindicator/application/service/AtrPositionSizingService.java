package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.AtrOrderPreview;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AtrPositionSizingService {
    private static final int SCALE = 8;

    public BigDecimal calculateAtr(List<Candle> candles, int period) {
        if (candles == null || candles.size() <= period) {
            throw new IllegalArgumentException("ATR 계산을 위한 캔들 수가 부족합니다.");
        }

        List<BigDecimal> trueRanges = new ArrayList<>();
        for (int i = 1; i < candles.size(); i++) {
            Candle current = candles.get(i);
            Candle previous = candles.get(i - 1);

            BigDecimal highLow = current.getHigh().subtract(current.getLow()).abs();
            BigDecimal highClose = current.getHigh().subtract(previous.getClose()).abs();
            BigDecimal lowClose = current.getLow().subtract(previous.getClose()).abs();

            trueRanges.add(highLow.max(highClose).max(lowClose));
        }

        BigDecimal atr = average(trueRanges.subList(0, period));
        for (int i = period; i < trueRanges.size(); i++) {
            atr = atr.multiply(BigDecimal.valueOf(period - 1L))
                    .add(trueRanges.get(i))
                    .divide(BigDecimal.valueOf(period), SCALE, RoundingMode.HALF_UP);
        }
        return atr;
    }

    public AtrOrderPreview preview(String symbol, String side, String interval, Integer atrPeriod,
                                   BigDecimal availableBalance, BigDecimal entryPrice, BigDecimal atr,
                                   BigDecimal riskPercent, BigDecimal atrMultiplier, BigDecimal leverage) {
        BigDecimal normalizedRiskPercent = defaultIfNull(riskPercent, BigDecimal.ONE);
        BigDecimal normalizedAtrMultiplier = defaultIfNull(atrMultiplier, BigDecimal.ONE);
        BigDecimal normalizedLeverage = defaultIfNull(leverage, BigDecimal.ONE);

        if (entryPrice == null || entryPrice.signum() <= 0) {
            throw new IllegalArgumentException("진입 가격은 0보다 커야 합니다.");
        }
        if (atr == null || atr.signum() <= 0) {
            throw new IllegalArgumentException("ATR 값은 0보다 커야 합니다.");
        }
        if (availableBalance == null || availableBalance.signum() <= 0) {
            throw new IllegalArgumentException("가용 자산은 0보다 커야 합니다.");
        }
        if (normalizedLeverage.signum() <= 0) {
            throw new IllegalArgumentException("배율은 0보다 커야 합니다.");
        }

        BigDecimal stopDistance = atr.multiply(normalizedAtrMultiplier);
        BigDecimal riskAmount = availableBalance.multiply(normalizedRiskPercent)
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
        BigDecimal quantity = riskAmount.divide(stopDistance, SCALE, RoundingMode.HALF_UP);
        BigDecimal notional = quantity.multiply(entryPrice);
        BigDecimal requiredMargin = notional.divide(normalizedLeverage, SCALE, RoundingMode.HALF_UP);

        boolean isLong = "BUY".equalsIgnoreCase(side);
        BigDecimal stopPrice = isLong ? entryPrice.subtract(stopDistance) : entryPrice.add(stopDistance);
        BigDecimal targetPrice = isLong ? entryPrice.add(stopDistance) : entryPrice.subtract(stopDistance);

        return new AtrOrderPreview(
                symbol,
                side == null ? "BUY" : side.toUpperCase(Locale.ROOT),
                interval,
                atrPeriod,
                availableBalance,
                entryPrice,
                atr,
                normalizedAtrMultiplier,
                stopDistance,
                normalizedRiskPercent,
                riskAmount,
                normalizedLeverage,
                quantity,
                notional,
                requiredMargin,
                stopPrice,
                targetPrice,
                riskAmount,
                riskAmount
        );
    }

    private BigDecimal average(List<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            sum = sum.add(value);
        }
        return sum.divide(BigDecimal.valueOf(values.size()), SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultIfNull(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }
}
