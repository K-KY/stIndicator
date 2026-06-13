package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.infra.connector.entity.ManagedStopHistoryEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ManagedStopHistoryResponseDto(
        Long id,
        Long managedPositionId,
        String symbol,
        String side,
        LocalDateTime changedAt,
        BigDecimal currentPrice,
        BigDecimal previousStopPrice,
        BigDecimal newStopPrice,
        String triggerBasis,
        BigDecimal triggerValue,
        BigDecimal triggerPrice,
        BigDecimal unrealizedPnl,
        BigDecimal pnlPercent,
        BigDecimal priceChangePercent,
        String protectType,
        BigDecimal protectValue,
        BigDecimal protectedAmount,
        BigDecimal protectedMarginPercent,
        String reason
) {
    public static ManagedStopHistoryResponseDto from(ManagedStopHistoryEntity entity) {
        return new ManagedStopHistoryResponseDto(
                entity.getId(),
                entity.getManagedPosition().getId(),
                entity.getSymbol(),
                entity.getSide(),
                entity.getChangedAt(),
                entity.getCurrentPrice(),
                entity.getPreviousStopPrice(),
                entity.getNewStopPrice(),
                entity.getTriggerBasis() == null ? null : entity.getTriggerBasis().name(),
                entity.getTriggerValue(),
                entity.getTriggerPrice(),
                entity.getUnrealizedPnl(),
                entity.getPnlPercent(),
                entity.getPriceChangePercent(),
                entity.getProtectType() == null ? null : entity.getProtectType().name(),
                entity.getProtectValue(),
                entity.getProtectedAmount(),
                entity.getProtectedMarginPercent(),
                entity.getReason() == null ? null : entity.getReason().name()
        );
    }
}
