package st.indicator.stindicator.presentation.ws.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import st.indicator.stindicator.application.service.MarketSubscriptionService;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.infra.ws.MultiPlexManager;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.dto.SymbolMonitorDto;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiPlexHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(MultiPlexHandler.class);
    private static final String SUBSCRIBE = "SUBSCRIBE";
    private static final String UNSUBSCRIBE = "UNSUBSCRIBE";
    private final ObjectMapper objectMapper;
    private final MonitorService monitorService;
    private final MultiPlexManager multiPlexManager;
    private final MarketSubscriptionService marketSubscriptionService;

    public MultiPlexHandler(ObjectMapper objectMapper, MonitorService monitorService, MultiPlexManager multiPlexManager,
                            MarketSubscriptionService marketSubscriptionService) {
        this.objectMapper = objectMapper;
        this.monitorService = monitorService;
        this.multiPlexManager = multiPlexManager;
        this.marketSubscriptionService = marketSubscriptionService;
    }

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        restoreStoredSubscriptions(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 클라이언트 → 서버 메시지
        SymbolMonitorDto req = objectMapper.readValue(message.getPayload(), SymbolMonitorDto.class);
        log.info("received request: {}", req.getSymbols());
        if (Objects.equals(req.getType(), SUBSCRIBE)) {
            log.info("subscribe request: {}", req.getSymbols());
            monitorService.subscribe(session, req);
            marketSubscriptionService.subscribe(sessionUserId(session), req.getSymbols(), req.getInterval());
            req.getSymbols().forEach(symbol -> {
                multiPlexManager.subscribeKline(symbol, req.getInterval());
                multiPlexManager.subscribeKline(symbol, "1d");
                multiPlexManager.subscribeTicker(symbol);
            });
        }
        if (Objects.equals(req.getType(), UNSUBSCRIBE)) {
            marketSubscriptionService.unsubscribe(sessionUserId(session), req.getSymbols());
            monitorService.unsubscribe(session, req)
                    .forEach(multiPlexManager::unsubscribe);
        }
    }

    @Override//지금까지 호출 된적 없음 언제 호출되는지 모르겠음
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("disconnect session: {}", session.getId());
        sessions.remove(session); // 세션 제거
        monitorService.unsubscribe(session)
                .forEach(multiPlexManager::unsubscribe);
    }

    // 서버 → 클라이언트
    public void broadcast(KlineEventDTO msg) {
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private Long sessionUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get(SessionUser.USER_ID);
        return userId instanceof Long id ? id : null;
    }

    private void restoreStoredSubscriptions(WebSocketSession session) {
        Long userId = sessionUserId(session);
        if (userId == null) {
            return;
        }

        marketSubscriptionService.list(userId).forEach(subscription -> {
            String symbol = subscription.getSymbol();
            String interval = subscription.getInterval();
            monitorService.subscribe(session, new SymbolMonitorDto(SUBSCRIBE, java.util.List.of(symbol), interval));
            multiPlexManager.subscribeKline(symbol, interval);
            multiPlexManager.subscribeKline(symbol, "1d");
            multiPlexManager.subscribeTicker(symbol);
        });
    }

}
