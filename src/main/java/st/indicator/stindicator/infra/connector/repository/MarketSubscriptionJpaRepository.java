package st.indicator.stindicator.infra.connector.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import st.indicator.stindicator.infra.connector.entity.MarketSubscriptionEntity;

import java.util.List;
import java.util.Optional;

public interface MarketSubscriptionJpaRepository extends JpaRepository<MarketSubscriptionEntity, Long> {
    List<MarketSubscriptionEntity> findAllByUser_IdOrderBySymbolAsc(Long userId);

    Optional<MarketSubscriptionEntity> findByUser_IdAndSymbol(Long userId, String symbol);

    void deleteByUser_IdAndSymbol(Long userId, String symbol);
}
