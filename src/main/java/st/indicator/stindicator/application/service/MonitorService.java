package st.indicator.stindicator.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import st.indicator.stindicator.infra.connector.repository.MonitorRepository;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.dto.SymbolMonitorDto;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class MonitorService {
    private final MonitorRepository monitorRepository;
    private final Map<String, Set<WebSocketSession>> subscribers = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    public void subscribe(WebSocketSession session, SymbolMonitorDto req) {
        List<String> symbols = req.getSymbols();
        for (String symbol : symbols) {
            Set<WebSocketSession> sessions = subscribers.getOrDefault(symbol, new HashSet<>());
            sessions.add(session);
            subscribers.put(symbol, sessions);

            //다음 데이터가 오기전 최신 데이터 푸시
            KlineEventDTO latest = monitorRepository.poll(symbol);
            if (latest != null) {
                try {
                    logger.info("Subscribe Symbol, Push Latest {} Data", symbol);
                    session.sendMessage(
                            new TextMessage(objectMapper.writeValueAsString(latest))
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }

    public void push(KlineEventDTO dto) {
        Set<WebSocketSession> sessions = subscribers.get(dto.getSymbol());
        monitorRepository.push(dto);//최신 데이터 저장

        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(
                                new TextMessage(objectMapper.writeValueAsString(dto))
                        );
                        return;
                    }
                    logger.info("Send Massage Failed Disconnect Session : {}", session.getId());
                    sessions.remove(session);
                } catch (Exception e) {
                    e.printStackTrace();

                }
            });
        }
    }

    public void unsubscribe(WebSocketSession session, SymbolMonitorDto req) {
        logger.info("Subscribe Symbol, DisConnect Session: {}, request Symbol: {}", session.getId(), req.getSymbols());

        req.getSymbols().forEach(symbol -> {
            subscribers.values().forEach(sessions -> {
                sessions.remove(session);
            });
        });
    }
    public void unsubscribe(WebSocketSession session) {
        logger.info("Subscribe Symbol, DisConnect Session: {}", session.getId());
        subscribers.values().forEach(set -> set.remove(session));
    }
}
