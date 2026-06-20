package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.domain.entity.ManagedPositionStatus;
import st.indicator.stindicator.domain.entity.TradeExecutionMode;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;

import java.util.List;
import java.util.Optional;

public interface ManagedPositionJpaRepository extends JpaRepository<ManagedPositionEntity, Long> {
    Optional<ManagedPositionEntity> findFirstByCloseOrderId(String closeOrderId);
    boolean existsByUserIdAndSymbolAndStatusInAndExecutionMode(
            Long userId, String symbol, List<ManagedPositionStatus> statuses, TradeExecutionMode executionMode);
    List<ManagedPositionEntity> findAllByUserIdAndSymbolAndStatusInAndExecutionMode(
            Long userId, String symbol, List<ManagedPositionStatus> statuses, TradeExecutionMode executionMode);
    List<ManagedPositionEntity> findAllByStatusOrderByOpenedAtDesc(ManagedPositionStatus status);
    List<ManagedPositionEntity> findAllByUserIdAndStatusOrderByOpenedAtDesc(Long userId, ManagedPositionStatus status);
    List<ManagedPositionEntity> findAllByStatusInOrderByOpenedAtDesc(List<ManagedPositionStatus> statuses);
    List<ManagedPositionEntity> findAllByUserIdAndStatusInOrderByOpenedAtDesc(Long userId, List<ManagedPositionStatus> statuses);
    List<ManagedPositionEntity> findAllByStatusInOrderByClosedAtDesc(List<ManagedPositionStatus> statuses);
    List<ManagedPositionEntity> findAllByUserIdAndStatusInOrderByClosedAtDesc(Long userId, List<ManagedPositionStatus> statuses);
    List<ManagedPositionEntity> findAllBySymbolAndStatus(String symbol, ManagedPositionStatus status);
}
