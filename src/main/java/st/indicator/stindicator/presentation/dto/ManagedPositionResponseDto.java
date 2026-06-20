package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.service.ManagedRaiseStopCalculator;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record ManagedPositionResponseDto(
        Long id,
        String symbol,
        String side,
        String closeSide,
        String entryOrderId,
        String closeOrderId,
        BigDecimal entryPrice,
        BigDecimal quantity,
        BigDecimal currentPrice,
        BigDecimal closePrice,
        BigDecimal unrealizedPnl,
        BigDecimal realizedPnl,
        BigDecimal initialStopPrice,
        BigDecimal currentStopPrice,
        BigDecimal targetPrice,
        BigDecimal possibleLoss,
        BigDecimal possibleProfit,
        BigDecimal leverage,
        BigDecimal requiredMargin,
        String stopTriggerBasis,
        String takeProfitTriggerBasis,
        BigDecimal stopPriceMovePercent,
        BigDecimal takeProfitPriceMovePercent,
        BigDecimal stopPnlPercent,
        BigDecimal takeProfitPnlPercent,
        BigDecimal configuredRiskPercent,
        BigDecimal currentStopPriceDistancePercent,
        BigDecimal currentStopMarginPercent,
        BigDecimal profitRatePercent,
        BigDecimal priceChangePercent,
        BigDecimal realizedPnlPercent,
        BigDecimal liquidationPrice,
        BigDecimal atr,
        BigDecimal atrMultiplier,
        BigDecimal riskPercent,
        String mode,
        boolean raiseActivated,
        String raiseTriggerType,
        BigDecimal raiseTriggerValue,
        String raiseStopType,
        BigDecimal raiseStopValue,
        BigDecimal nextStopTriggerPrice,
        BigDecimal predictedNextStopPrice,
        BigDecimal predictedNextStopAmount,
        BigDecimal predictedNextStopPercent,
        String executionMode,
        String status,
        String closeReason,
        String managementEvents,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        Long holdingSeconds
) {
    public static ManagedPositionResponseDto from(ManagedPositionEntity entity) {
        String side = "BUY".equalsIgnoreCase(entity.getEntrySide()) ? "LONG" : "SHORT";
        boolean raisingStopOnly = entity.getMode() != null && "RAISING_STOP_ONLY".equals(entity.getMode().name());
        ManagedRaiseStopCalculator.RaiseStopPlan raisePlan = ManagedRaiseStopCalculator.calculate(entity);
        return new ManagedPositionResponseDto(entity.getId(), entity.getSymbol(), side, entity.getCloseSide(),
                entity.getEntryOrderId(), entity.getCloseOrderId(), entity.getEntryPrice(), entity.getQuantity(),
                entity.getCurrentPrice(), entity.getClosePrice(), entity.getUnrealizedPnl(), entity.getRealizedPnl(),
                entity.getInitialStopPrice(), entity.getCurrentStopPrice(), raisingStopOnly ? null : entity.getTargetPrice(),
                entity.getPossibleLoss(), raisingStopOnly ? null : entity.getPossibleProfit(), entity.getLeverage(), entity.getRequiredMargin(),
                entity.getStopTriggerBasis() == null ? null : entity.getStopTriggerBasis().name(),
                raisingStopOnly || entity.getTakeProfitTriggerBasis() == null ? null : entity.getTakeProfitTriggerBasis().name(),
                priceMovePercent(entity.getEntryPrice(), entity.getCurrentStopPrice()),
                raisingStopOnly ? null : priceMovePercent(entity.getEntryPrice(), entity.getTargetPrice()),
                pnlPercent(entity.getPossibleLoss(), entity.getRequiredMargin()),
                raisingStopOnly ? null : pnlPercent(entity.getPossibleProfit(), entity.getRequiredMargin()),
                entity.getRiskPercent(),
                priceMovePercent(entity.getEntryPrice(), entity.getCurrentStopPrice()),
                currentStopMarginPercent(entity),
                profitRatePercent(entity), priceChangePercent(entity), realizedPnlPercent(entity), liquidationPrice(entity),
                entity.getAtr(), entity.getAtrMultiplier(), entity.getRiskPercent(), entity.getMode() == null ? null : entity.getMode().name(),
                entity.isRaiseActivated(),
                entity.getRaiseTriggerType() == null ? null : entity.getRaiseTriggerType().name(),
                entity.getRaiseTriggerValue(),
                entity.getRaiseStopType() == null ? null : entity.getRaiseStopType().name(),
                entity.getRaiseStopValue(),
                raisePlan == null ? null : raisePlan.triggerPrice(),
                raisePlan == null ? null : raisePlan.nextStopPrice(),
                raisePlan == null ? null : raisePlan.protectedAmount(),
                raisePlan == null ? null : raisePlan.protectedPercent(),
                entity.getExecutionMode().name(),
                entity.getStatus().name(),
                entity.getCloseReason() == null ? null : entity.getCloseReason().name(),
                entity.getManagementEvents(), entity.getOpenedAt(), entity.getClosedAt(), holdingSeconds(entity));
    }

    private static BigDecimal profitRatePercent(ManagedPositionEntity entity) {
        if (entity.getUnrealizedPnl() == null
                || entity.getRequiredMargin() == null
                || entity.getRequiredMargin().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return entity.getUnrealizedPnl()
                .divide(entity.getRequiredMargin(), 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal realizedPnlPercent(ManagedPositionEntity entity) {
        if (entity.getRealizedPnl() == null
                || entity.getRequiredMargin() == null
                || entity.getRequiredMargin().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return entity.getRealizedPnl()
                .divide(entity.getRequiredMargin(), 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal priceChangePercent(ManagedPositionEntity entity) {
        if (entity.getCurrentPrice() == null
                || entity.getEntryPrice() == null
                || entity.getEntryPrice().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal move = "BUY".equalsIgnoreCase(entity.getEntrySide())
                ? entity.getCurrentPrice().subtract(entity.getEntryPrice())
                : entity.getEntryPrice().subtract(entity.getCurrentPrice());
        return move.divide(entity.getEntryPrice(), 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static Long holdingSeconds(ManagedPositionEntity entity) {
        if (entity.getOpenedAt() == null || entity.getClosedAt() == null) {
            return null;
        }
        return java.time.Duration.between(entity.getOpenedAt(), entity.getClosedAt()).getSeconds();
    }

    private static BigDecimal liquidationPrice(ManagedPositionEntity entity) {
        if (entity.getEntryPrice() == null
                || entity.getLeverage() == null
                || entity.getLeverage().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal marginDistance = entity.getEntryPrice().divide(entity.getLeverage(), 8, RoundingMode.HALF_UP);
        if ("BUY".equalsIgnoreCase(entity.getEntrySide())) {
            return entity.getEntryPrice().subtract(marginDistance);
        }
        return entity.getEntryPrice().add(marginDistance);
    }

    private static BigDecimal priceMovePercent(BigDecimal entryPrice, BigDecimal triggerPrice) {
        if (entryPrice == null || triggerPrice == null || entryPrice.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return triggerPrice.subtract(entryPrice).abs()
                .divide(entryPrice, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal pnlPercent(BigDecimal amount, BigDecimal requiredMargin) {
        if (amount == null || requiredMargin == null || requiredMargin.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(requiredMargin, 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal currentStopMarginPercent(ManagedPositionEntity entity) {
        if (entity.getEntryPrice() == null
                || entity.getCurrentStopPrice() == null
                || entity.getQuantity() == null
                || entity.getRequiredMargin() == null
                || entity.getRequiredMargin().signum() == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal pnlAtStop = "BUY".equalsIgnoreCase(entity.getEntrySide())
                ? entity.getCurrentStopPrice().subtract(entity.getEntryPrice()).multiply(entity.getQuantity())
                : entity.getEntryPrice().subtract(entity.getCurrentStopPrice()).multiply(entity.getQuantity());
        return pnlAtStop.divide(entity.getRequiredMargin(), 8, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

}
