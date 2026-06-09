package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class AtrPositionSizingService {
    private static final Logger log = LoggerFactory.getLogger(AtrPositionSizingService.class);
    private static final int SCALE = 8;

    public BigDecimal calculateAtr(List<Candle> candles, int period) {
        log.info("flow calculateAtr start candleCount={}, period={}", candles == null ? 0 : candles.size(), period);
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
        log.info("flow calculateAtr done period={}, atr={}", period, atr);
        return atr;
    }

    public AtrOrderPreview preview(String symbol, String side, String interval, Integer atrPeriod,
                                   BigDecimal availableBalance, BigDecimal entryPrice, BigDecimal atr,
                                   BigDecimal riskPercent, BigDecimal atrMultiplier, BigDecimal leverage) {
        return preview(symbol, side, interval, atrPeriod, availableBalance, entryPrice, atr, riskPercent,
                atrMultiplier, leverage, TriggerBasis.PRICE_PERCENT, TriggerBasis.PRICE_PERCENT);
    }

    public AtrOrderPreview preview(String symbol, String side, String interval, Integer atrPeriod,
                                   BigDecimal availableBalance, BigDecimal entryPrice, BigDecimal atr,
                                   BigDecimal riskPercent, BigDecimal atrMultiplier, BigDecimal leverage,
                                   TriggerBasis stopTriggerBasis, TriggerBasis takeProfitTriggerBasis) {
        log.info("flow buildAtrPreview start symbol={}, side={}, interval={}, atrPeriod={}, availableBalance={}, entryPrice={}, atr={}, riskPercent={}, atrMultiplier={}, leverage={}",
                symbol, side, interval, atrPeriod, availableBalance, entryPrice, atr, riskPercent, atrMultiplier, leverage);
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

        BigDecimal stopDistance = atr.multiply(normalizedAtrMultiplier);//이 값만큼 더한값 혹은 뺀값을 청산 조건으로 사용
        BigDecimal riskAmount = availableBalance.multiply(normalizedRiskPercent)
                .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);//이 값만큼 현재 보유 자산 대비 리스크 지정
        BigDecimal quantity = riskAmount.divide(stopDistance, SCALE, RoundingMode.HALF_UP);//진입 해야할 포지션 개수
        BigDecimal notional = quantity.multiply(entryPrice);//레버리지 적용후 전체 보유량
        BigDecimal requiredMargin = notional.divide(normalizedLeverage, SCALE, RoundingMode.HALF_UP);//최종적으로 필요한 증거금

        //매도인지 매수인지 판단 후 매수인 경우 riskAmount%만큼 하락한 경우 청산 반대의 경우 같은 금액만큼 오른경우 청산
        boolean isLong = "BUY".equalsIgnoreCase(side);
        BigDecimal stopPrice = isLong ? entryPrice.subtract(stopDistance) : entryPrice.add(stopDistance);//청산
        BigDecimal targetPrice = isLong ? entryPrice.add(stopDistance) : entryPrice.subtract(stopDistance);//목표가
        BigDecimal possibleLoss = riskAmount;
        BigDecimal possibleProfit = riskAmount;

        if (stopTriggerBasis == TriggerBasis.PNL_PERCENT) {
            possibleLoss = requiredMargin.multiply(normalizedRiskPercent)
                    .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
            BigDecimal priceMoveForLoss = possibleLoss.divide(quantity, SCALE, RoundingMode.HALF_UP);
            stopPrice = isLong ? entryPrice.subtract(priceMoveForLoss) : entryPrice.add(priceMoveForLoss);
        }
        if (takeProfitTriggerBasis == TriggerBasis.PNL_PERCENT) {
            possibleProfit = requiredMargin.multiply(normalizedRiskPercent)
                    .divide(BigDecimal.valueOf(100), SCALE, RoundingMode.HALF_UP);
            BigDecimal priceMoveForProfit = possibleProfit.divide(quantity, SCALE, RoundingMode.HALF_UP);
            targetPrice = isLong ? entryPrice.add(priceMoveForProfit) : entryPrice.subtract(priceMoveForProfit);
        }

        AtrOrderPreview preview = new AtrOrderPreview(
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
                possibleLoss,
                possibleProfit,
                stopTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : stopTriggerBasis,
                takeProfitTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : takeProfitTriggerBasis,
                priceMovePercent(entryPrice, stopPrice),
                priceMovePercent(entryPrice, targetPrice),
                marginPnlPercent(possibleLoss, requiredMargin),
                marginPnlPercent(possibleProfit, requiredMargin)
        );
        log.info("flow buildAtrPreview done symbol={}, quantity={}, requiredMargin={}, stopPrice={}, targetPrice={}",
                preview.getSymbol(), preview.getQuantity(), preview.getRequiredMargin(),
                preview.getStopPrice(), preview.getTargetPrice());
        return preview;
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

    private BigDecimal priceMovePercent(BigDecimal entryPrice, BigDecimal triggerPrice) {
        if (entryPrice == null || triggerPrice == null || entryPrice.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return triggerPrice.subtract(entryPrice).abs()
                .divide(entryPrice, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private BigDecimal marginPnlPercent(BigDecimal amount, BigDecimal requiredMargin) {
        if (amount == null || requiredMargin == null || requiredMargin.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(requiredMargin, SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }
}
