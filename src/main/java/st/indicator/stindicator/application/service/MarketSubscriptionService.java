package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.indicator.stindicator.domain.entity.MarketSubscription;
import st.indicator.stindicator.infra.connector.entity.MarketSubscriptionEntity;
import st.indicator.stindicator.infra.connector.repository.MarketSubscriptionJpaRepository;

import java.util.List;
import java.util.Locale;

@Service
public class MarketSubscriptionService {
    private final MarketSubscriptionJpaRepository marketSubscriptionJpaRepository;

    public MarketSubscriptionService(MarketSubscriptionJpaRepository marketSubscriptionJpaRepository) {
        this.marketSubscriptionJpaRepository = marketSubscriptionJpaRepository;
    }

    @Transactional
    public void subscribe(Long userId, List<String> symbols, String interval) {
        if (userId == null || symbols == null) {
            return;
        }
        for (String symbol : symbols) {
            String normalizedSymbol = normalizeSymbol(symbol);
            marketSubscriptionJpaRepository.findByUser_IdAndSymbol(userId, normalizedSymbol)
                    .ifPresentOrElse(
                            entity -> entity.updateInterval(interval),
                            () -> marketSubscriptionJpaRepository.save(MarketSubscriptionEntity.create(userId, normalizedSymbol, interval))
                    );
        }
    }

    @Transactional
    public void unsubscribe(Long userId, List<String> symbols) {
        if (userId == null || symbols == null) {
            return;
        }
        symbols.forEach(symbol -> marketSubscriptionJpaRepository.deleteByUser_IdAndSymbol(userId, normalizeSymbol(symbol)));
    }

    public List<MarketSubscription> list(Long userId) {
        return marketSubscriptionJpaRepository.findAllByUser_IdOrderBySymbolAsc(userId)
                .stream()
                .map(MarketSubscriptionEntity::toDomain)
                .toList();
    }

    private String normalizeSymbol(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("심볼은 필수입니다.");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }
}
