package st.indicator.stindicator.infra.connector.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;

import java.util.HashMap;

@Repository
public class MonitorRepository {
    private final HashMap<String, KlineEventDTO> lastRepository = new HashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(MonitorRepository.class);

    public void push(KlineEventDTO dto) {
        lastRepository.put(dto.getSymbol(), dto);
    }

    public KlineEventDTO poll(String symbol) {
        return lastRepository.get(symbol);
    }
}
