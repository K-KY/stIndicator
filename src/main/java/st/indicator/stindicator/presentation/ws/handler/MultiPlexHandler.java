package st.indicator.stindicator.presentation.ws.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import st.indicator.stindicator.application.service.MonitorService;
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

    public MultiPlexHandler(ObjectMapper objectMapper, MonitorService monitorService) {

        this.objectMapper = objectMapper;
        this.monitorService = monitorService;
    }

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 클라이언트 → 서버 메시지
        SymbolMonitorDto req = objectMapper.readValue(message.getPayload(), SymbolMonitorDto.class);
        log.info("received request: {}", req.getSymbols());
        if (Objects.equals(req.getType(), SUBSCRIBE)) {
            monitorService.subscribe(session, req);
        }
        if (Objects.equals(req.getType(), UNSUBSCRIBE)) {
            monitorService.unsubscribe(session, req);
        }
    }

    @Override//지금까지 호출 된적 없음 언제 호출되는지 모르겠음
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("disconnect session: {}", session.getId());
        sessions.remove(session); // 세션 제거
        monitorService.unsubscribe(session);//
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

}
