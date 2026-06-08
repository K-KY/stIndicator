package st.indicator.stindicator.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import st.indicator.stindicator.infra.connector.repository.MonitorRepository;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.ws.dto.MonitorSocketEventDto;
import st.indicator.stindicator.presentation.dto.SymbolMonitorDto;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitorService {
    private final MonitorRepository monitorRepository;
    private final Map<String, Set<WebSocketSession>> streamSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    public void subscribe(WebSocketSession session, SymbolMonitorDto req) {
        logger.info("start subscribe: {}", req.getSymbols());
        List<String> symbols = req.getSymbols();
        for (String symbol : symbols) {
            String streamKey = toKlineStreamKey(symbol, req.getInterval());
            String dailyStreamKey = toKlineStreamKey(symbol, "1d");
            String tickerStreamKey = toTickerStreamKey(symbol);
            streamSubscribers.computeIfAbsent(streamKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            streamSubscribers.computeIfAbsent(dailyStreamKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            streamSubscribers.computeIfAbsent(tickerStreamKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(streamKey);
            sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(dailyStreamKey);
            sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(tickerStreamKey);

            KlineEventDTO latest = monitorRepository.poll(streamKey);
            if (latest != null) {
                try {
                    logger.info("Subscribe Symbol, Push Latest {} Data", streamKey);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(latest)));
                } catch (Exception e) {
                    logger.warn("latest kline send failed session={}, stream={}", session.getId(), streamKey, e);
                }
            }
        }
    }

    public void push(KlineEventDTO dto) {
        String streamKey = toKlineStreamKey(dto.getSymbol(), dto.getKline().getInterval());
        Set<WebSocketSession> sessions = streamSubscribers.get(streamKey);
        monitorRepository.push(streamKey, dto);//최신 데이터 저장

        if (sessions != null) {
            sessions.removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(dto)));
                        return false;
                    }
                    logger.info("Send Massage Failed Disconnect Session : {}", session.getId());
                    return true;
                } catch (Exception e) {
                    logger.warn("kline event send failed session={}, stream={}", session.getId(), streamKey, e);
                    return true;
                }
            });
        }
    }

    public void pushTicker(String symbol, String payload) {
        String streamKey = toTickerStreamKey(symbol);
        Set<WebSocketSession> sessions = streamSubscribers.get(streamKey);

        if (sessions != null) {
            sessions.removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(payload));
                        return false;
                    }
                    logger.info("Send Massage Failed Disconnect Session : {}", session.getId());
                    return true;
                } catch (Exception e) {
                    logger.warn("ticker event send failed session={}, stream={}", session.getId(), streamKey, e);
                    return true;
                }
            });
        }
    }

    public Set<String> unsubscribe(WebSocketSession session, SymbolMonitorDto req) {
        logger.info("Subscribe Symbol, DisConnect Session: {}, request Symbol: {}", session.getId(), req.getSymbols());
        Set<String> releasedStreams = new HashSet<>();
        Set<String> sessionStreams = sessionSubscriptions.get(session.getId());

        req.getSymbols().forEach(symbol -> {
            Set<String> streamKeys = Set.of(toKlineStreamKey(symbol, req.getInterval()), toKlineStreamKey(symbol, "1d"), toTickerStreamKey(symbol));

            streamKeys.forEach(streamKey -> {
                Set<WebSocketSession> sessions = streamSubscribers.get(streamKey);
                if (sessions == null) {
                    return;
                }
                sessions.remove(session);
                if (sessionStreams != null) {
                    sessionStreams.remove(streamKey);
                }
                if (sessions.isEmpty()) {
                    streamSubscribers.remove(streamKey);
                    releasedStreams.add(streamKey);
                }
            });
        });

        if (sessionStreams != null && sessionStreams.isEmpty()) {
            sessionSubscriptions.remove(session.getId());
        }

        return releasedStreams;
    }

    public Set<String> unsubscribe(WebSocketSession session) {
        logger.info("Subscribe Symbol, DisConnect Session: {}", session.getId());
        Set<String> releasedStreams = new HashSet<>();
        Set<String> sessionStreams = sessionSubscriptions.remove(session.getId());

        streamSubscribers.forEach((streamKey, sessions) -> {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                streamSubscribers.remove(streamKey);
                releasedStreams.add(streamKey);
            }
        });

        if (sessionStreams != null) {
            sessionStreams.forEach(streamKey -> {
                if (!streamSubscribers.containsKey(streamKey)) {
                    releasedStreams.add(streamKey);
                }
            });
        }

        return releasedStreams;
    }

    public void publishMonitorEvent(String symbol, MonitorSocketEventDto event) {
        Set<WebSocketSession> targetSessions = new HashSet<>();
        Set<WebSocketSession> staleSessions = new HashSet<>();
        String normalizedSymbol = symbol.toUpperCase();

        streamSubscribers.forEach((streamKey, sessions) -> {
            if (streamKey.startsWith(normalizedSymbol.toLowerCase() + "@kline_")) {
                targetSessions.addAll(sessions);
            }
        });

        if (targetSessions.isEmpty()) {
            return;
        }

        targetSessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(event)));
                    return;
                }
                staleSessions.add(session);
            } catch (Exception e) {
                logger.warn("monitor event send failed session={}, symbol={}", session.getId(), symbol, e);
                staleSessions.add(session);
            }
        });

        staleSessions.forEach(this::unsubscribe);
    }

    public boolean hasSubscribers(String symbol) {
        String prefix = symbol.toLowerCase() + "@kline_";
        return streamSubscribers.entrySet().stream()
                .anyMatch(entry -> entry.getKey().startsWith(prefix) && !entry.getValue().isEmpty());
    }

    public boolean hasSubscribers(String symbol, String interval) {
        Set<WebSocketSession> sessions = streamSubscribers.get(toKlineStreamKey(symbol, interval));
        return sessions != null && !sessions.isEmpty();
    }

    public boolean hasStreamSubscribers(String streamKey) {
        Set<WebSocketSession> sessions = streamSubscribers.get(streamKey);
        if (sessions == null) {
            return false;
        }
        sessions.removeIf(session -> !session.isOpen());
        if (sessions.isEmpty()) {
            streamSubscribers.remove(streamKey);
            return false;
        }
        return true;
    }

    private String toKlineStreamKey(String symbol, String interval) {
        String normalizedInterval = interval == null || interval.isBlank() ? "1m" : interval.toLowerCase();
        return symbol.toLowerCase() + "@kline_" + normalizedInterval;
    }

    private String toTickerStreamKey(String symbol) {
        return symbol.toLowerCase() + "@ticker";
    }
}
