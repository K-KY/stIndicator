package st.indicator.stindicator.infra.connector.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class MonitorRepository {
    private final Map<String, KlineEventDTO> lastRepository = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(MonitorRepository.class);

    public void push(String streamKey, KlineEventDTO dto) {
        lastRepository.put(streamKey, dto);
    }

    public KlineEventDTO poll(String streamKey) {
        return lastRepository.get(streamKey);
    }
}
