package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionJournalEntity;

import java.util.List;
import java.util.Optional;

public interface ManagedPositionJournalJpaRepository extends JpaRepository<ManagedPositionJournalEntity, Long> {
    Optional<ManagedPositionJournalEntity> findByManagedPosition_Id(Long managedPositionId);
    List<ManagedPositionJournalEntity> findAllByOrderByUpdatedAtDesc();
    List<ManagedPositionJournalEntity> findAllByManagedPosition_SymbolIgnoreCaseOrderByUpdatedAtDesc(String symbol);
}
