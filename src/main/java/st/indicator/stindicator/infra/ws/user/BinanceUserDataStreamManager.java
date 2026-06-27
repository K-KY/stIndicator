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
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketOutbound;
import st.indicator.stindicator.presentation.ws.publisher.AccountPositionUpdateEvent;
import st.indicator.stindicator.presentation.ws.publisher.OrderTradeUpdateEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private volatile Connection channel;
    private volatile ScheduledFuture<?> pingTask;
    private volatile ScheduledFuture<?> keepAliveTask;
    private final AtomicBoolean connecting = new AtomicBoolean();
    private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
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
        keepAliveTask = executor.scheduleAtFixedRate(this::keepAlive, 30, 30, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        cancelPingTask();
        cancelTask(keepAliveTask, "user data keepalive task canceled");
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        executor.shutdownNow();
    }

    private void refreshAndConnect() {
        if (shuttingDown) {
            return;
        }
        if (!connecting.compareAndSet(false, true)) {
            log.info("user data reconnect skipped: already reconnecting");
            return;
        }
        try {
            String listenKey = restClient.start();
            connect(listenKey);
        } catch (RuntimeException e) {
            connecting.set(false);
            log.warn("binance user data stream connect failed, retry soon", e);
            requestReconnect("listenKey creation failed");
        }
    }

    private void connect(String listenKey) {
        cancelPingTask();
        disposeConnection();
        connection = HttpClient.create()
                .websocket()
                .uri(PRIVATE_WS_BASE_URL + listenKey)
                .handle((in, out) -> {
                    in.withConnection(activeChannel -> this.channel = activeChannel);
                    this.outbound = out;
                    connecting.set(false);
                    reconnectScheduled.set(false);
                    startPingTask();
                    log.info("binance user data stream connected");
                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doFinally(this::handleConnectionClosed)
                            .then();
                })
                .subscribe(
                        ignored -> { },
                        error -> {
                            connecting.set(false);
                            log.warn("binance user data stream connection failed", error);
                            requestReconnect("connection failed");
                        },
                        () -> {
                            connecting.set(false);
                            requestReconnect("connection completed");
                        }
                );
    }

    private void keepAlive() {
        try {
            restClient.keepAlive();
        } catch (RuntimeException e) {
            log.warn("binance user data stream keepalive failed, recreate listenKey", e);
            requestReconnect("listenKey keepalive failed");
        }
    }

    private void sendPing() {
        if (!isConnectionActive()) {
            log.info("ping skipped: user data connection not active");
            requestReconnect("ping found inactive connection");
            return;
        }
        outbound.sendObject(Mono.just(new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1}))))
                .then()
                .subscribe(
                        ignored -> { },
                        error -> {
                            log.warn("user data websocket ping failed", error);
                            requestReconnect("ping failed");
                        },
                        () -> log.debug("user data websocket ping success")
                );
    }

    private boolean isConnectionActive() {
        return !shuttingDown
                && connection != null
                && !connection.isDisposed()
                && outbound != null
                && channel != null
                && !channel.isDisposed()
                && channel.channel().isActive();
    }

    private void startPingTask() {
        cancelPingTask();
        if (shuttingDown) {
            return;
        }
        try {
            pingTask = executor.scheduleAtFixedRate(this::sendPing, 15, 15, TimeUnit.SECONDS);
            log.info("user data ping task started");
        } catch (RejectedExecutionException error) {
            if (!shuttingDown) {
                log.error("user data ping task start failed", error);
            }
        }
    }

    private void cancelPingTask() {
        ScheduledFuture<?> task = pingTask;
        pingTask = null;
        cancelTask(task, "user data ping task canceled");
    }

    private void cancelTask(ScheduledFuture<?> task, String message) {
        if (task != null && !task.isDone()) {
            task.cancel(false);
            log.info(message);
        }
    }

    private void handleConnectionClosed(reactor.core.publisher.SignalType signal) {
        outbound = null;
        channel = null;
        cancelPingTask();
        if (!shuttingDown) {
            log.warn("binance user data stream closed signal={}", signal);
            requestReconnect("connection closed signal=" + signal);
        }
    }

    private void requestReconnect(String reason) {
        if (shuttingDown) {
            return;
        }
        if (!reconnectScheduled.compareAndSet(false, true)) {
            log.info("user data reconnect skipped: already reconnecting");
            return;
        }
        log.warn("user data reconnect requested reason={}", reason);
        cancelPingTask();
        disposeConnection();
        try {
            executor.schedule(() -> {
                reconnectScheduled.set(false);
                refreshAndConnect();
            }, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException error) {
            reconnectScheduled.set(false);
            if (!shuttingDown) {
                log.error("user data reconnect scheduling failed", error);
            }
        }
    }

    private void disposeConnection() {
        Disposable current = connection;
        if (current != null && !current.isDisposed()) {
            current.dispose();
        }
    }

    private void handleMessage(String message) {
        if (message == null || !message.trim().startsWith("{")) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(message);
            if (!root.has("e")) {
                return;
            }
            String eventType = root.get("e").asText();
            if ("ACCOUNT_UPDATE".equals(eventType)) {
                publishAccountPositionUpdate(root);
                return;
            }
            if (!"ORDER_TRADE_UPDATE".equals(eventType)) {
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

    private void publishAccountPositionUpdate(JsonNode root) {
        JsonNode account = root.get("a");
        if (account == null || account.isNull()) {
            return;
        }
        JsonNode positions = account.get("P");
        if (positions == null || !positions.isArray()) {
            return;
        }
        Set<String> symbols = new LinkedHashSet<>();
        for (JsonNode position : positions) {
            String symbol = text(position, "s");
            if (symbol != null && !symbol.isBlank()) {
                symbols.add(symbol.toUpperCase(Locale.ROOT));
            }
        }
        if (symbols.isEmpty()) {
            return;
        }
        AccountPositionUpdateEvent event = new AccountPositionUpdateEvent(
                symbols,
                text(account, "m"),
                longValue(root, "E"),
                longValue(root, "T")
        );
        eventPublisher.publishEvent(event);
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
