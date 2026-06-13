package st.indicator.stindicator.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import st.indicator.stindicator.infra.connector.repository.MonitorRepository;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.dto.SymbolMonitorDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionResponseDto;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MonitorService {
    private final MonitorRepository monitorRepository;
    private final Map<String, Set<WebSocketSession>> streamSubscribers = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> sessionSendLocks = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger logger = LoggerFactory.getLogger(MonitorService.class);

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    public void registerSession(WebSocketSession session) {
        activeSessions.add(session);
    }

    public void unregisterSession(WebSocketSession session) {
        activeSessions.remove(session);
        sessionSendLocks.remove(session.getId());
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
                    sendText(session, objectMapper.writeValueAsString(latest), "latestKline", streamKey, false);
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
                String payload = objectMapper.writeValueAsString(dto);
                return !sendText(session, payload, "kline", streamKey, false);
            });
        }
    }

    public void pushTicker(String symbol, String payload) {
        String streamKey = toTickerStreamKey(symbol);
        pushRawPayload(streamKey, payload, "ticker");
    }

    public void pushPositionUpdate(Long userId, String symbol, ManagedPositionResponseDto position) {
        String eventType = position.status().equals("CLOSED") || position.status().equals("FAILED")
                ? ("TEST".equals(position.executionMode()) ? "TEST_POSITION_CLOSED" : "POSITION_CLOSED")
                : "POSITION_UPDATED";
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", eventType,
                "positionId", position.id(),
                "status", position.status(),
                "position", position
        ));
        activeSessions.removeIf(session -> {
            if (!session.isOpen()) {
                return true;
            }
            Object sessionUserId = session.getAttributes().get(SessionUser.USER_ID);
            if (!(sessionUserId instanceof Long id) || !id.equals(userId)) {
                return false;
            }
            return !sendText(session, payload, "positionUpdate", symbol, false);
        });
    }

    public void subscribeDepth(WebSocketSession session, List<String> symbols) {
        logger.info("start depth subscribe: {}", symbols);
        for (String symbol : symbols) {
            String streamKey = toDepthStreamKey(symbol);
            streamSubscribers.computeIfAbsent(streamKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
            sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(streamKey);
        }
    }

    public Set<String> unsubscribeDepth(WebSocketSession session, List<String> symbols) {
        Set<String> releasedStreams = new HashSet<>();
        Set<String> sessionStreams = sessionSubscriptions.get(session.getId());

        symbols.forEach(symbol -> {
            String streamKey = toDepthStreamKey(symbol);
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

        if (sessionStreams != null && sessionStreams.isEmpty()) {
            sessionSubscriptions.remove(session.getId());
        }

        return releasedStreams;
    }

    public void pushDepth(String symbol, String payload) {
        pushRawPayload(toDepthStreamKey(symbol), payload, "depth");
    }

    private void pushRawPayload(String streamKey, String payload, String eventName) {
        Set<WebSocketSession> sessions = streamSubscribers.get(streamKey);

        if (sessions != null) {
            sessions.removeIf(session -> {
                boolean dropIfBusy = "depth".equals(eventName);
                return !sendText(session, payload, eventName, streamKey, dropIfBusy);
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
            sessionSendLocks.remove(session.getId());
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
        sessionSendLocks.remove(session.getId());
        activeSessions.remove(session);

        return releasedStreams;
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

    private String toDepthStreamKey(String symbol) {
        return symbol.toLowerCase() + "@depth20@100ms";
    }

    private boolean sendText(WebSocketSession session, String payload, String eventName, String streamKey, boolean dropIfBusy) {
        if (!session.isOpen()) {
            logger.info("Send Message Failed Disconnect Session : {}", session.getId());
            sessionSendLocks.remove(session.getId());
            return false;
        }

        ReentrantLock lock = sessionSendLocks.computeIfAbsent(session.getId(), ignored -> new ReentrantLock());
        boolean locked = lock.tryLock();
        if (!locked) {
            if (!dropIfBusy) {
                lock.lock();
            } else {
                logger.debug("drop busy websocket frame event={}, session={}, stream={}", eventName, session.getId(), streamKey);
                return true;
            }
        }

        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IllegalStateException e) {
            logger.debug("websocket session busy event={}, session={}, stream={}", eventName, session.getId(), streamKey);
            return true;
        } catch (Exception e) {
            logger.warn("{} event send failed session={}, stream={}", eventName, session.getId(), streamKey, e);
            sessionSendLocks.remove(session.getId());
            return false;
        } finally {
            lock.unlock();
        }
    }
}
