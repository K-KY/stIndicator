package st.indicator.stindicator.infra.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketOutbound;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.ws.dto.BinanceMarkPriceMessage;
import st.indicator.stindicator.presentation.ws.publisher.MonitorEventPublisher;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MultiPlexManager {
    private static final Logger log = LoggerFactory.getLogger(MultiPlexManager.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(15);

    private static final String MARKET_WS_URL = "wss://fstream.binance.com/market/stream";
    private static final String PUBLIC_WS_URL = "wss://fstream.binance.com/public/stream";
    private final MonitorService monitorService;
    private final MonitorEventPublisher monitorEventPublisher;

    private final ObjectMapper objectMapper;

    // 현재 구독 중인 stream 목록
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    private final Set<String> publicSubscriptions = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>(Instant.now());
    private final ScheduledExecutorService watchdogExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile WebsocketOutbound outbound;
    private volatile WebsocketOutbound publicOutbound;
    private volatile Disposable connection;
    private volatile Disposable publicConnection;
    private volatile Connection marketChannel;
    private volatile Connection publicChannel;
    private volatile ScheduledFuture<?> marketPingTask;
    private volatile ScheduledFuture<?> publicPingTask;
    private final AtomicBoolean marketConnecting = new AtomicBoolean();
    private final AtomicBoolean publicConnecting = new AtomicBoolean();
    private final AtomicBoolean marketReconnectScheduled = new AtomicBoolean();
    private final AtomicBoolean publicReconnectScheduled = new AtomicBoolean();
    private volatile boolean shuttingDown;

    public MultiPlexManager(MonitorService monitorService,
                            MonitorEventPublisher monitorEventPublisher,
                            ObjectMapper objectMapper) {
        this.monitorService = monitorService;
        this.monitorEventPublisher = monitorEventPublisher;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        connect();
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        cancelMarketPingTask();
        cancelPublicPingTask();
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        if (publicConnection != null && !publicConnection.isDisposed()) {
            publicConnection.dispose();
        }
        watchdogExecutor.shutdownNow();
    }

    private void connect() {
        if (shuttingDown || !marketConnecting.compareAndSet(false, true)) {
            log.info("market reconnect skipped: already reconnecting");
            return;
        }
        lastMessageAt.set(Instant.now());
        this.connection = HttpClient.create()
                .websocket()
                .uri(MARKET_WS_URL)
                .handle((in, out) -> {
                    in.withConnection(channel -> this.marketChannel = channel);
                    this.outbound = out;
                    marketConnecting.set(false);
                    marketReconnectScheduled.set(false);
                    startMarketPingTask();
                    resendSubscriptions();

                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doFinally(this::handleMarketConnectionClosed)
                            .then();
                })
                .subscribe(
                        ignored -> { },
                        error -> {
                            marketConnecting.set(false);
                            log.warn("binance market websocket connection failed", error);
                            requestMarketReconnect("connection failed");
                        },
                        () -> {
                            marketConnecting.set(false);
                            requestMarketReconnect("connection completed");
                        }
                );
    }

    private void connectPublic() {
        if (shuttingDown || publicSubscriptions.isEmpty()) {
            return;
        }
        if (!publicConnecting.compareAndSet(false, true)) {
            log.info("public reconnect skipped: already reconnecting");
            return;
        }
        lastMessageAt.set(Instant.now());
        this.publicConnection = HttpClient.create()
                .websocket()
                .uri(PUBLIC_WS_URL)
                .handle((in, out) -> {
                    in.withConnection(channel -> this.publicChannel = channel);
                    this.publicOutbound = out;
                    publicConnecting.set(false);
                    publicReconnectScheduled.set(false);
                    startPublicPingTask();
                    resendPublicSubscriptions();

                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doFinally(this::handlePublicConnectionClosed)
                            .then();
                })
                .subscribe(
                        ignored -> { },
                        error -> {
                            publicConnecting.set(false);
                            log.warn("binance public websocket connection failed", error);
                            requestPublicReconnect("connection failed");
                        },
                        () -> {
                            publicConnecting.set(false);
                            requestPublicReconnect("connection completed");
                        }
                );
    }

    private void reconnect() {
        requestMarketReconnect("reconnect requested");
    }

    //구독
    public void subscribe(String stream) {
        if (subscriptions.add(stream)) {
            log.info("subscribe upstream stream={}", stream);
            sendMessage(buildSubscribeMessage(stream));
        }
    }

    //구독 취소
    public void unsubscribe(String stream) {
        if (subscriptions.remove(stream)) {
            log.info("unsubscribe upstream stream={}", stream);
            sendMessage(buildUnsubscribeMessage(stream));
            return;
        }
        if (publicSubscriptions.remove(stream)) {
            log.info("unsubscribe public upstream stream={}", stream);
            sendPublicMessage(buildUnsubscribeMessage(stream));
        }
    }

    public void subscribeKline(String symbol, String interval) {
        String normalizedInterval = interval == null || interval.isBlank() ? "1m" : interval.toLowerCase();
        subscribe(symbol.toLowerCase() + "@kline_" + normalizedInterval);
    }

    public void unsubscribeKline(String symbol, String interval) {
        String normalizedInterval = interval == null || interval.isBlank() ? "1m" : interval.toLowerCase();
        unsubscribe(symbol.toLowerCase() + "@kline_" + normalizedInterval);
    }

    public void subscribeMarkPrice(String symbol) {
        subscribe(symbol.toLowerCase() + "@markPrice@1s");
    }

    public void unsubscribeMarkPrice(String symbol) {
        unsubscribe(symbol.toLowerCase() + "@markPrice@1s");
    }

    public void subscribeTicker(String symbol) {
        subscribe(symbol.toLowerCase() + "@ticker");
    }

    public void unsubscribeTicker(String symbol) {
        unsubscribe(symbol.toLowerCase() + "@ticker");
    }

    public void subscribeDepth(String symbol) {
        String stream = symbol.toLowerCase() + "@depth20@100ms";
        if (publicSubscriptions.add(stream)) {
            log.info("subscribe public upstream stream={}", stream);
            ensurePublicConnection();
            sendPublicMessage(buildSubscribeMessage(stream));
        }
    }

    public void unsubscribeDepth(String symbol) {
        String stream = symbol.toLowerCase() + "@depth20@100ms";
        if (publicSubscriptions.remove(stream)) {
            log.info("unsubscribe public upstream stream={}", stream);
            sendPublicMessage(buildUnsubscribeMessage(stream));
        }
    }

    public int activeConnectionCount() {
        int marketCount = outbound == null || connection == null || connection.isDisposed() ? 0 : 1;
        int publicCount = publicOutbound == null || publicConnection == null || publicConnection.isDisposed() ? 0 : 1;
        return marketCount + publicCount;
    }

    public int subscriptionCount() {
        return subscriptions.size() + publicSubscriptions.size();
    }

    private void resendSubscriptions() {
        subscriptions.forEach(stream ->
                sendMessage(buildSubscribeMessage(stream))
        );
    }

    private void resendPublicSubscriptions() {
        publicSubscriptions.forEach(stream ->
                sendPublicMessage(buildSubscribeMessage(stream))
        );
    }

    private void sendMessage(String msg) {
        if (!isActive(connection, marketChannel, outbound)) {
            log.debug("market message skipped: connection not active");
            return;
        }
        outbound.sendString(Mono.just(msg)).then().subscribe(
                ignored -> { },
                error -> {
                    log.warn("market websocket message failed", error);
                    requestMarketReconnect("message failed");
                }
        );
    }

    private void sendPublicMessage(String msg) {
        if (!isActive(publicConnection, publicChannel, publicOutbound)) {
            log.debug("public message skipped: connection not active");
            return;
        }
        publicOutbound.sendString(Mono.just(msg)).then().subscribe(
                ignored -> { },
                error -> {
                    log.warn("public websocket message failed", error);
                    requestPublicReconnect("message failed");
                }
        );
    }

    private void ensurePublicConnection() {
        if (publicConnection == null || publicConnection.isDisposed()) {
            connectPublic();
        }
    }

    private void sendMarketPing() {
        if (subscriptions.isEmpty()) {
            return;
        }
        if (!isActive(connection, marketChannel, outbound)) {
            log.info("ping skipped: market connection not active");
            requestMarketReconnect("ping found inactive connection");
            return;
        }
        outbound.sendObject(Mono.just(new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1}))))
                .then()
                .subscribe(
                        ignored -> { },
                        error -> {
                            log.warn("market websocket ping failed", error);
                            requestMarketReconnect("ping failed");
                        },
                        () -> log.debug("market websocket ping success")
                );
    }

    private void sendPublicPing() {
        if (publicSubscriptions.isEmpty()) {
            return;
        }
        if (!isActive(publicConnection, publicChannel, publicOutbound)) {
            log.info("ping skipped: public connection not active");
            requestPublicReconnect("ping found inactive connection");
            return;
        }
        publicOutbound.sendObject(Mono.just(new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{1}))))
                .then()
                .subscribe(
                        ignored -> { },
                        error -> {
                            log.warn("public websocket ping failed", error);
                            requestPublicReconnect("ping failed");
                        },
                        () -> log.debug("public websocket ping success")
                );
    }

    private boolean isActive(Disposable socketConnection, Connection channel, WebsocketOutbound socketOutbound) {
        return !shuttingDown
                && socketConnection != null
                && !socketConnection.isDisposed()
                && socketOutbound != null
                && channel != null
                && !channel.isDisposed()
                && channel.channel().isActive();
    }

    private void startMarketPingTask() {
        cancelMarketPingTask();
        if (shuttingDown) {
            return;
        }
        try {
            marketPingTask = watchdogExecutor.scheduleAtFixedRate(
                    this::sendMarketPing, HEARTBEAT_INTERVAL.toSeconds(), HEARTBEAT_INTERVAL.toSeconds(), TimeUnit.SECONDS);
            log.info("market ping task started");
        } catch (RejectedExecutionException error) {
            if (!shuttingDown) {
                log.error("market ping task start failed", error);
            }
        }
    }

    private void startPublicPingTask() {
        cancelPublicPingTask();
        if (shuttingDown) {
            return;
        }
        try {
            publicPingTask = watchdogExecutor.scheduleAtFixedRate(
                    this::sendPublicPing, HEARTBEAT_INTERVAL.toSeconds(), HEARTBEAT_INTERVAL.toSeconds(), TimeUnit.SECONDS);
            log.info("public ping task started");
        } catch (RejectedExecutionException error) {
            if (!shuttingDown) {
                log.error("public ping task start failed", error);
            }
        }
    }

    private void cancelMarketPingTask() {
        ScheduledFuture<?> task = marketPingTask;
        marketPingTask = null;
        if (task != null && !task.isDone()) {
            task.cancel(false);
            log.info("market ping task canceled");
        }
    }

    private void cancelPublicPingTask() {
        ScheduledFuture<?> task = publicPingTask;
        publicPingTask = null;
        if (task != null && !task.isDone()) {
            task.cancel(false);
            log.info("public ping task canceled");
        }
    }

    private void handleMarketConnectionClosed(reactor.core.publisher.SignalType signalType) {
        outbound = null;
        marketChannel = null;
        cancelMarketPingTask();
        requestMarketReconnect("connection closed signal=" + signalType);
    }

    private void handlePublicConnectionClosed(reactor.core.publisher.SignalType signalType) {
        publicOutbound = null;
        publicChannel = null;
        cancelPublicPingTask();
        if (!publicSubscriptions.isEmpty()) {
            requestPublicReconnect("connection closed signal=" + signalType);
        }
    }

    private void requestMarketReconnect(String reason) {
        if (shuttingDown) {
            return;
        }
        if (!marketReconnectScheduled.compareAndSet(false, true)) {
            log.info("market reconnect skipped: already reconnecting");
            return;
        }
        log.warn("market reconnect requested reason={}", reason);
        cancelMarketPingTask();
        dispose(connection);
        try {
            watchdogExecutor.schedule(() -> {
                marketReconnectScheduled.set(false);
                connect();
            }, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException error) {
            marketReconnectScheduled.set(false);
            if (!shuttingDown) {
                log.error("market reconnect scheduling failed", error);
            }
        }
    }

    private void requestPublicReconnect(String reason) {
        if (shuttingDown || publicSubscriptions.isEmpty()) {
            return;
        }
        if (!publicReconnectScheduled.compareAndSet(false, true)) {
            log.info("public reconnect skipped: already reconnecting");
            return;
        }
        log.warn("public reconnect requested reason={}", reason);
        cancelPublicPingTask();
        dispose(publicConnection);
        try {
            watchdogExecutor.schedule(() -> {
                publicReconnectScheduled.set(false);
                connectPublic();
            }, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException error) {
            publicReconnectScheduled.set(false);
            if (!shuttingDown) {
                log.error("public reconnect scheduling failed", error);
            }
        }
    }

    private void dispose(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private String buildSubscribeMessage(String stream) {
        return """
            {
              "method": "SUBSCRIBE",
              "params": ["%s"],
              "id": %d
            }
        """.formatted(stream, System.currentTimeMillis());
    }

    private String buildUnsubscribeMessage(String stream) {
        return """
            {
              "method": "UNSUBSCRIBE",
              "params": ["%s"],
              "id": %d
            }
        """.formatted(stream, System.currentTimeMillis());
    }


    /*
    {"e":"kline",
    "E":1776605903512,
    "s":"BTCUSDT",
        "k":{
            "t":1776605880000,
            "T":1776605939999,
            "s":"BTCUSDT",
            "i":"1m",
            "f":7578829466,
            "L":7578830256,
            "o":"75949.90",
            "c":"75928.50",
            "h":"75950.00",
            "l":"75923.30",
            "v":"21.102",
            "n":790,
            "x":false,
             "q":"1602383.36460",
             "V":"6.964",
             "Q":"528784.69440",
             "B":"0"
             }
     }
     */
    private void handleMessage(String msg) {
        if (msg == null) {
            return;
        }

        String trimmed = msg.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            log.debug("ignore non-json websocket frame");
            return;
        }

        JsonNode node;
        try {
            node = objectMapper.readTree(trimmed);
        } catch (Exception e) {
            log.warn("multiplex message parse failed payload={}", trimmed, e);
            return;
        }

        try {
            lastMessageAt.set(Instant.now());
            JsonNode payload = node.has("data") ? node.get("data") : node;
            String payloadJson = objectMapper.writeValueAsString(payload);

            //구독 응답 메시지 무시
            if (node.has("result")) {
                return;
            }

            //새로운 캔들 데이터가 들어왔을 때 처리
            if (payload.has("e") && "kline".equals(payload.get("e").asString())) {
                KlineEventDTO dto = objectMapper.readValue(payloadJson, KlineEventDTO.class);
                monitorService.push(dto);
                return;
            }

            if (payload.has("e") && "24hrTicker".equals(payload.get("e").asString())) {
                monitorService.pushTicker(payload.get("s").asString(), payloadJson);
                return;
            }

            if (payload.has("e") && "depthUpdate".equals(payload.get("e").asString())) {
                monitorService.pushDepth(payload.get("s").asString(), payloadJson);
                return;
            }

            //실시간 가격 업데이트 처리
            if (payload.has("e") && "markPriceUpdate".equals(payload.get("e").asString())) {
                BinanceMarkPriceMessage dto = objectMapper.readValue(payloadJson, BinanceMarkPriceMessage.class);
                monitorEventPublisher.publishPriceTick(dto.getSymbol(), dto.getMarkPrice(), dto.getEventTime());
            }
        } catch (Exception e) {
            log.warn("multiplex message handling failed payload={}", trimmed, e);
        }
    }
}
