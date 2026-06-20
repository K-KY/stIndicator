package st.indicator.stindicator.application.service;

import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ManagedRaiseStopCalculator {
    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

    private ManagedRaiseStopCalculator() {
    }

    public static RaiseStopPlan calculate(ManagedPositionEntity position) {
        return calculate(position, position == null ? null : position.getCurrentPrice(),
                position == null ? null : position.getUnrealizedPnl());
    }

    public static RaiseStopPlan calculate(ManagedPositionEntity position, BigDecimal currentPrice, BigDecimal currentPnl) {
        if (position == null
                || !position.isRaiseStopEnabled()
                || position.getEntryPrice() == null
                || position.getQuantity() == null
                || position.getQuantity().signum() <= 0
                || position.getCurrentStopPrice() == null) {
            return null;
        }

        return position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT
                ? calculatePnlPlan(position, currentPnl)
                : calculatePricePlan(position, currentPrice);
    }

    public static boolean reached(ManagedPositionEntity position, BigDecimal currentPrice,
                                  BigDecimal currentPnl, RaiseStopPlan plan) {
        if (plan == null) {
            return false;
        }
        if (valueOrZero(position.getRaiseTriggerValue()).signum() <= 0) {
            return currentPnl != null && currentPnl.signum() > 0;
        }
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            return currentPnl != null && currentPnl.compareTo(plan.triggerPnl()) >= 0;
        }
        return isLong(position)
                ? currentPrice.compareTo(plan.triggerPrice()) >= 0
                : currentPrice.compareTo(plan.triggerPrice()) <= 0;
    }

    private static RaiseStopPlan calculatePricePlan(ManagedPositionEntity position, BigDecimal currentPrice) {
        BigDecimal entryPrice = position.getEntryPrice();
        BigDecimal triggerIncrement = position.getRaiseTriggerType() == RaiseStopType.AMOUNT
                ? valueOrZero(position.getRaiseTriggerValue())
                : entryPrice.multiply(valueOrZero(position.getRaiseTriggerValue()))
                .divide(ONE_HUNDRED, 18, RoundingMode.HALF_UP);
        if (triggerIncrement.signum() <= 0) {
            return null;
        }
        BigDecimal currentMove = currentPrice == null
                ? BigDecimal.ZERO
                : favorableMove(position, currentPrice).max(BigDecimal.ZERO);
        BigDecimal triggerMove = steppedProgress(currentMove, triggerIncrement);
        if (triggerMove.signum() <= 0) {
            triggerMove = triggerIncrement;
        }
        BigDecimal triggerPrice = priceAtMove(position, triggerMove);

        BigDecimal nextStopPrice;
        if (position.getRaiseStopType() == RaiseStopType.AMOUNT) {
            BigDecimal distance = valueOrZero(position.getRaiseStopValue());
            nextStopPrice = isLong(position)
                    ? triggerPrice.subtract(distance)
                    : triggerPrice.add(distance);
        } else {
            BigDecimal protectedAtTrigger = triggerMove
                    .multiply(valueOrZero(position.getRaiseStopValue()))
                    .divide(ONE_HUNDRED, 18, RoundingMode.HALF_UP);
            nextStopPrice = priceAtMove(position, protectedAtTrigger);
        }
        nextStopPrice = monotonicStop(position, nextStopPrice);
        BigDecimal protectedPnl = pnlAtPrice(position, nextStopPrice);
        return new RaiseStopPlan(triggerPrice, pnlAtPrice(position, triggerPrice),
                nextStopPrice, protectedPnl, marginPercent(position, protectedPnl));
    }

    private static RaiseStopPlan calculatePnlPlan(ManagedPositionEntity position, BigDecimal currentPnl) {
        if (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0) {
            return null;
        }
        BigDecimal triggerIncrement = position.getRaiseTriggerType() == RaiseStopType.AMOUNT
                ? valueOrZero(position.getRaiseTriggerValue())
                : position.getRequiredMargin().multiply(valueOrZero(position.getRaiseTriggerValue()))
                .divide(ONE_HUNDRED, 18, RoundingMode.HALF_UP);
        if (triggerIncrement.signum() <= 0) {
            return null;
        }
        BigDecimal favorablePnl = currentPnl == null ? BigDecimal.ZERO : currentPnl.max(BigDecimal.ZERO);
        BigDecimal triggerPnl = steppedProgress(favorablePnl, triggerIncrement);
        if (triggerPnl.signum() <= 0) {
            triggerPnl = triggerIncrement;
        }
        BigDecimal triggerPrice = priceAtPnl(position, triggerPnl);

        BigDecimal nextProtectedPnl = position.getRaiseStopType() == RaiseStopType.AMOUNT
                ? valueOrZero(position.getRaiseStopValue())
                : triggerPnl.multiply(valueOrZero(position.getRaiseStopValue()))
                .divide(ONE_HUNDRED, 18, RoundingMode.HALF_UP);
        BigDecimal nextStopPrice = monotonicStop(position, priceAtPnl(position, nextProtectedPnl));
        nextProtectedPnl = pnlAtPrice(position, nextStopPrice);
        return new RaiseStopPlan(triggerPrice, triggerPnl, nextStopPrice,
                nextProtectedPnl, marginPercent(position, nextProtectedPnl));
    }

    private static BigDecimal steppedProgress(BigDecimal currentProgress, BigDecimal triggerIncrement) {
        if (currentProgress == null || currentProgress.signum() <= 0
                || triggerIncrement == null || triggerIncrement.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return currentProgress.divideToIntegralValue(triggerIncrement).multiply(triggerIncrement);
    }

    private static BigDecimal favorableMove(ManagedPositionEntity position, BigDecimal price) {
        return isLong(position)
                ? price.subtract(position.getEntryPrice())
                : position.getEntryPrice().subtract(price);
    }

    private static BigDecimal priceAtMove(ManagedPositionEntity position, BigDecimal move) {
        return isLong(position)
                ? position.getEntryPrice().add(move)
                : position.getEntryPrice().subtract(move);
    }

    private static BigDecimal priceAtPnl(ManagedPositionEntity position, BigDecimal pnl) {
        BigDecimal move = pnl.divide(position.getQuantity(), 18, RoundingMode.HALF_UP);
        return priceAtMove(position, move);
    }

    private static BigDecimal pnlAtPrice(ManagedPositionEntity position, BigDecimal price) {
        return favorableMove(position, price).multiply(position.getQuantity());
    }

    private static BigDecimal monotonicStop(ManagedPositionEntity position, BigDecimal candidate) {
        BigDecimal current = position.getCurrentStopPrice();
        if (isLong(position)) {
            return candidate.compareTo(current) > 0 ? candidate : current;
        }
        return candidate.compareTo(current) < 0 ? candidate : current;
    }

    private static BigDecimal marginPercent(ManagedPositionEntity position, BigDecimal amount) {
        if (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(position.getRequiredMargin(), 18, RoundingMode.HALF_UP)
                .multiply(ONE_HUNDRED);
    }

    private static BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static boolean isLong(ManagedPositionEntity position) {
        return "BUY".equalsIgnoreCase(position.getEntrySide());
    }

    public record RaiseStopPlan(
            BigDecimal triggerPrice,
            BigDecimal triggerPnl,
            BigDecimal nextStopPrice,
            BigDecimal protectedAmount,
            BigDecimal protectedPercent
    ) {
    }
}
