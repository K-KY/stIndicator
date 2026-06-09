package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.infra.connector.entity.PendingOrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PendingOrderResponseDto(
        Long id,
        String symbol,
        String side,
        String orderId,
        String clientOrderId,
        BigDecimal entryPrice,
        BigDecimal quantity,
        BigDecimal stopPrice,
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
        BigDecimal priceMovePercentForStop,
        BigDecimal priceMovePercentForTakeProfit,
        BigDecimal marginPnlPercentForStop,
        BigDecimal marginPnlPercentForTakeProfit,
        BigDecimal atr,
        BigDecimal atrMultiplier,
        BigDecimal riskPercent,
        String mode,
        boolean raiseStopEnabled,
        String raiseTriggerType,
        BigDecimal raiseTriggerValue,
        String raiseStopType,
        BigDecimal raiseStopValue,
        BigDecimal predictedNextStopPrice,
        BigDecimal predictedNextStopAmount,
        BigDecimal predictedNextStopPercent,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PendingOrderResponseDto from(PendingOrderEntity entity) {
        boolean raisingStopOnly = "RAISING_STOP_ONLY".equals(entity.getMode().name());
        return new PendingOrderResponseDto(entity.getId(), entity.getSymbol(), entity.getSide(),
                entity.getOrderId(), entity.getClientOrderId(), entity.getEntryPrice(), entity.getQuantity(),
                entity.getStopPrice(), raisingStopOnly ? null : entity.getTargetPrice(),
                entity.getPossibleLoss(), raisingStopOnly ? null : entity.getPossibleProfit(),
                entity.getLeverage(), entity.getRequiredMargin(), entity.getStopTriggerBasis().name(),
                raisingStopOnly || entity.getTakeProfitTriggerBasis() == null ? null : entity.getTakeProfitTriggerBasis().name(),
                priceMovePercent(entity.getEntryPrice(), entity.getStopPrice()),
                raisingStopOnly ? null : priceMovePercent(entity.getEntryPrice(), entity.getTargetPrice()),
                pnlPercent(entity.getPossibleLoss(), entity.getRequiredMargin()),
                raisingStopOnly ? null : pnlPercent(entity.getPossibleProfit(), entity.getRequiredMargin()),
                priceMovePercent(entity.getEntryPrice(), entity.getStopPrice()),
                raisingStopOnly ? null : priceMovePercent(entity.getEntryPrice(), entity.getTargetPrice()),
                pnlPercent(entity.getPossibleLoss(), entity.getRequiredMargin()),
                raisingStopOnly ? null : pnlPercent(entity.getPossibleProfit(), entity.getRequiredMargin()),
                entity.getAtr(), entity.getAtrMultiplier(),
                entity.getRiskPercent(), entity.getMode().name(), entity.isRaiseStopEnabled(),
                entity.getRaiseTriggerType() == null ? null : entity.getRaiseTriggerType().name(),
                entity.getRaiseTriggerValue(),
                entity.getRaiseStopType() == null ? null : entity.getRaiseStopType().name(),
                entity.getRaiseStopValue(), predictedNextStopPrice(entity), predictedNextStopAmount(entity),
                predictedNextStopPercent(entity), entity.getStatus().name(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private static BigDecimal priceMovePercent(BigDecimal entryPrice, BigDecimal triggerPrice) {
        if (entryPrice == null || triggerPrice == null || entryPrice.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return triggerPrice.subtract(entryPrice).abs()
                .divide(entryPrice, 8, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal pnlPercent(BigDecimal amount, BigDecimal requiredMargin) {
        if (amount == null || requiredMargin == null || requiredMargin.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return amount.divide(requiredMargin, 8, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private static BigDecimal predictedNextStopPrice(PendingOrderEntity entity) {
        if (!"RAISING_STOP_ONLY".equals(entity.getMode().name())
                || entity.getEntryPrice() == null
                || entity.getQuantity() == null
                || entity.getQuantity().signum() <= 0
                || entity.getRaiseStopType() == null
                || entity.getRaiseStopValue() == null) {
            return null;
        }
        BigDecimal protectedAmount = predictedNextStopAmount(entity);
        if (protectedAmount == null) {
            return null;
        }
        BigDecimal priceMove = protectedAmount.divide(entity.getQuantity(), 8, java.math.RoundingMode.HALF_UP);
        return "BUY".equalsIgnoreCase(entity.getSide())
                ? entity.getEntryPrice().add(priceMove)
                : entity.getEntryPrice().subtract(priceMove);
    }

    private static BigDecimal predictedNextStopAmount(PendingOrderEntity entity) {
        if (!"RAISING_STOP_ONLY".equals(entity.getMode().name())
                || entity.getRaiseStopType() == null
                || entity.getRaiseStopValue() == null) {
            return null;
        }
        if ("AMOUNT".equals(entity.getRaiseStopType().name())) {
            return entity.getRaiseStopValue();
        }
        if (entity.getPossibleLoss() == null) {
            return null;
        }
        return entity.getPossibleLoss()
                .multiply(entity.getRaiseStopValue())
                .divide(new BigDecimal("100"), 8, java.math.RoundingMode.HALF_UP);
    }

    private static BigDecimal predictedNextStopPercent(PendingOrderEntity entity) {
        BigDecimal amount = predictedNextStopAmount(entity);
        return pnlPercent(amount, entity.getRequiredMargin());
    }
}
