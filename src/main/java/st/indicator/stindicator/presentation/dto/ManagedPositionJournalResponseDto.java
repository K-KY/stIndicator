package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionJournalEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

// 매매일지와 연결된 종료 포지션 요약을 함께 내려주는 응답 DTO
public record ManagedPositionJournalResponseDto(
        Long id,
        Long managedPositionId,
        Long userId,
        String title,
        String entryReason,
        String content,
        String review,
        String tags,
        String symbol,
        String side,
        String mode,
        String closeReason,
        BigDecimal realizedPnl,
        BigDecimal realizedPnlPercent,
        LocalDateTime openedAt,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ManagedPositionJournalResponseDto from(ManagedPositionJournalEntity entity) {
        ManagedPositionEntity position = entity.getManagedPosition();
        BigDecimal realizedPnlPercent = realizedPnlPercent(position);
        return new ManagedPositionJournalResponseDto(
                entity.getId(),
                position.getId(),
                entity.getUser() == null ? null : entity.getUser().getId(),
                entity.getTitle(),
                entity.getEntryReason(),
                entity.getContent(),
                entity.getReview(),
                entity.getTags(),
                position.getSymbol(),
                position.getEntrySide(),
                position.getMode() == null ? null : position.getMode().name(),
                position.getCloseReason() == null ? null : position.getCloseReason().name(),
                position.getRealizedPnl(),
                realizedPnlPercent,
                position.getOpenedAt(),
                position.getClosedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static BigDecimal realizedPnlPercent(ManagedPositionEntity position) {
        if (position.getRealizedPnl() == null
                || position.getRequiredMargin() == null
                || position.getRequiredMargin().signum() == 0) {
            return null;
        }
        return position.getRealizedPnl()
                .divide(position.getRequiredMargin(), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(6, RoundingMode.HALF_UP);
    }
}
