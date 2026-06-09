package st.indicator.stindicator.infra.ws.user;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketOutbound;
import st.indicator.stindicator.presentation.ws.publisher.OrderTradeUpdateEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class BinanceUserDataStreamManager {
    private static final Logger log = LoggerFactory.getLogger(BinanceUserDataStreamManager.class);
    private static final String PRIVATE_WS_BASE_URL = "wss://fstream.binance.com/private/ws/";
    private final BinanceUserDataStreamRestClient restClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile WebsocketOutbound outbound;
    private volatile Disposable connection;
    private volatile boolean shuttingDown;

    public BinanceUserDataStreamManager(BinanceUserDataStreamRestClient restClient,
                                        ObjectMapper objectMapper,
                                        ApplicationEventPublisher eventPublisher) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void start() {
        if (!restClient.isConfigured()) {
            log.warn("binance user data stream skipped because api key is not configured");
            return;
        }
        refreshAndConnect();
        executor.scheduleAtFixedRate(this::keepAlive, 30, 30, TimeUnit.MINUTES);
        executor.scheduleAtFixedRate(this::sendPing, 15, 15, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        executor.shutdownNow();
    }

    private void refreshAndConnect() {
        try {
            String listenKey = restClient.start();
            connect(listenKey);
            log.info("binance user data stream connected");
        } catch (RuntimeException e) {
            log.warn("binance user data stream connect failed, retry soon", e);
            executor.schedule(this::refreshAndConnect, 5, TimeUnit.SECONDS);
        }
    }

    private void connect(String listenKey) {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        connection = HttpClient.create()
                .websocket()
                .uri(PRIVATE_WS_BASE_URL + listenKey)
                .handle((in, out) -> {
                    this.outbound = out;
                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doFinally(signal -> {
                                this.outbound = null;
                                if (!shuttingDown) {
                                    log.warn("binance user data stream closed signal={}", signal);
                                    executor.schedule(this::refreshAndConnect, 1, TimeUnit.SECONDS);
                                }
                            })
                            .then();
                })
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30)))
                .subscribe();
    }

    private void keepAlive() {
        try {
            restClient.keepAlive();
        } catch (RuntimeException e) {
            log.warn("binance user data stream keepalive failed, recreate listenKey", e);
            refreshAndConnect();
        }
    }

    private void sendPing() {
        if (outbound != null) {
            outbound.sendObject(Mono.just(new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1}))))
                    .then()
                    .subscribe();
        }
    }

    private void handleMessage(String message) {
        if (message == null || !message.trim().startsWith("{")) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!root.has("e") || !"ORDER_TRADE_UPDATE".equals(root.get("e").asText())) {
                return;
            }
            JsonNode order = root.get("o");
            if (order == null || order.isNull()) {
                return;
            }
            OrderTradeUpdateEvent event = new OrderTradeUpdateEvent(
                    text(order, "s"),
                    text(order, "c"),
                    text(order, "S"),
                    text(order, "o"),
                    text(order, "x"),
                    text(order, "X"),
                    text(order, "i"),
                    decimal(order, "ap"),
                    decimal(order, "z"),
                    longValue(root, "E"),
                    longValue(root, "T")
            );
            eventPublisher.publishEvent(event);
        } catch (RuntimeException e) {
            log.warn("binance user data stream parse failed payload={}", message, e);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        String text = text(node, fieldName);
        return text == null || text.isBlank() ? BigDecimal.ZERO : new BigDecimal(text);
    }

    private long longValue(JsonNode node, String fieldName) {
        JsonNode value = node.get(fieldName);
        return value == null || value.isNull() ? 0L : value.asLong();
    }
}
