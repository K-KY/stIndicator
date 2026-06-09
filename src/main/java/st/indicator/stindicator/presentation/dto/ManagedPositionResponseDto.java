package st.indicator.stindicator.presentation.dto;

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
        BigDecimal profitRatePercent,
        BigDecimal liquidationPrice,
        String mode,
        boolean raiseActivated,
        String raiseStopType,
        BigDecimal raiseStopValue,
        BigDecimal predictedNextStopPrice,
        BigDecimal predictedNextStopAmount,
        BigDecimal predictedNextStopPercent,
        String status,
        String closeReason,
        LocalDateTime openedAt,
        LocalDateTime closedAt
) {
    public static ManagedPositionResponseDto from(ManagedPositionEntity entity) {
        String side = "BUY".equalsIgnoreCase(entity.getEntrySide()) ? "LONG" : "SHORT";
        boolean raisingStopOnly = "RAISING_STOP_ONLY".equals(entity.getMode().name());
        return new ManagedPositionResponseDto(entity.getId(), entity.getSymbol(), side, entity.getCloseSide(),
                entity.getEntryOrderId(), entity.getCloseOrderId(), entity.getEntryPrice(), entity.getQuantity(),
                entity.getCurrentPrice(), entity.getUnrealizedPnl(), entity.getRealizedPnl(),
                entity.getInitialStopPrice(), entity.getCurrentStopPrice(), raisingStopOnly ? null : entity.getTargetPrice(),
                entity.getPossibleLoss(), raisingStopOnly ? null : entity.getPossibleProfit(), entity.getLeverage(), entity.getRequiredMargin(),
                entity.getStopTriggerBasis().name(), raisingStopOnly || entity.getTakeProfitTriggerBasis() == null ? null : entity.getTakeProfitTriggerBasis().name(),
                priceMovePercent(entity.getEntryPrice(), entity.getCurrentStopPrice()),
                raisingStopOnly ? null : priceMovePercent(entity.getEntryPrice(), entity.getTargetPrice()),
                pnlPercent(entity.getPossibleLoss(), entity.getRequiredMargin()),
                raisingStopOnly ? null : pnlPercent(entity.getPossibleProfit(), entity.getRequiredMargin()),
                profitRatePercent(entity), liquidationPrice(entity), entity.getMode().name(),
                entity.isRaiseActivated(),
                entity.getRaiseStopType() == null ? null : entity.getRaiseStopType().name(),
                entity.getRaiseStopValue(), predictedNextStopPrice(entity), predictedNextStopAmount(entity),
                predictedNextStopPercent(entity), entity.getStatus().name(),
                entity.getCloseReason() == null ? null : entity.getCloseReason().name(),
                entity.getOpenedAt(), entity.getClosedAt());
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

    private static BigDecimal predictedNextStopPrice(ManagedPositionEntity entity) {
        BigDecimal amount = predictedNextStopAmount(entity);
        if (amount == null || entity.getQuantity() == null || entity.getQuantity().signum() <= 0) {
            return null;
        }
        BigDecimal priceMove = amount.divide(entity.getQuantity(), 8, RoundingMode.HALF_UP);
        return "BUY".equalsIgnoreCase(entity.getEntrySide())
                ? entity.getEntryPrice().add(priceMove)
                : entity.getEntryPrice().subtract(priceMove);
    }

    private static BigDecimal predictedNextStopAmount(ManagedPositionEntity entity) {
        if (!"RAISING_STOP_ONLY".equals(entity.getMode().name())
                || entity.getRaiseStopType() == null
                || entity.getRaiseStopValue() == null) {
            return null;
        }
        if ("AMOUNT".equals(entity.getRaiseStopType().name())) {
            return entity.getRaiseStopValue();
        }
        if (entity.getUnrealizedPnl() == null || entity.getUnrealizedPnl().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return entity.getUnrealizedPnl()
                .multiply(entity.getRaiseStopValue())
                .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal predictedNextStopPercent(ManagedPositionEntity entity) {
        return pnlPercent(predictedNextStopAmount(entity), entity.getRequiredMargin());
    }
}
