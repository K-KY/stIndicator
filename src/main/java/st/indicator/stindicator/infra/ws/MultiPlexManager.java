package st.indicator.stindicator.infra.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MultiPlexManager {
    private static final Logger log = LoggerFactory.getLogger(MultiPlexManager.class);
    private static final Duration HEARTBEAT_TIMEOUT = Duration.ofSeconds(30);

    private static final String WS_URL = "wss://fstream.binance.com/ws";
    private final MonitorService monitorService;
    private final MonitorEventPublisher monitorEventPublisher;

    private final ObjectMapper objectMapper;

    // 현재 구독 중인 stream 목록
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();
    private final AtomicReference<Instant> lastMessageAt = new AtomicReference<>(Instant.now());
    private final ScheduledExecutorService watchdogExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile WebsocketOutbound outbound;
    private volatile Disposable connection;

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
        watchdogExecutor.scheduleAtFixedRate(this::checkHeartbeat, 10, 10, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void shutdown() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        watchdogExecutor.shutdownNow();
    }

    private void connect() {
        lastMessageAt.set(Instant.now());
        this.connection = HttpClient.create()
                .websocket()
                .uri(WS_URL)
                .handle((in, out) -> {
                    this.outbound = out;
                    resendSubscriptions();

                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doFinally(signalType -> this.outbound = null)
                            .then();
                })
                .retryWhen(reactor.util.retry.Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(30))
                        .doBeforeRetry(signal -> log.warn("binance multiplex reconnect attempt={}", signal.totalRetriesInARow() + 1)))
                .subscribe();
    }

    private void reconnect() {
        if (connection != null && !connection.isDisposed()) {
            connection.dispose();
        }
        connect();
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

    private void resendSubscriptions() {
        subscriptions.forEach(stream ->
                sendMessage(buildSubscribeMessage(stream))
        );
    }

    private void sendMessage(String msg) {
        if (outbound != null) {
            outbound.sendString(Mono.just(msg)).then().subscribe();
        }
    }

    private void checkHeartbeat() {
        if (subscriptions.isEmpty()) {
            return;
        }
        Instant last = lastMessageAt.get();
        if (Duration.between(last, Instant.now()).compareTo(HEARTBEAT_TIMEOUT) > 0) {
            log.warn("binance multiplex heartbeat timeout, reconnect");
            reconnect();
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
        try {
            lastMessageAt.set(Instant.now());
            JsonNode node = objectMapper.readTree(msg);

            //구독 응답 메시지 무시
            if (node.has("result")) {
                return;
            }

            //새로운 캔들 데이터가 들어왔을 때 처리
            if (node.has("e") && "kline".equals(node.get("e").asString())) {
                KlineEventDTO dto = objectMapper.readValue(msg, KlineEventDTO.class);
                monitorService.push(dto);
                return;
            }

            //실시간 가격 업데이트 처리
            if (node.has("e") && "markPriceUpdate".equals(node.get("e").asString())) {
                BinanceMarkPriceMessage dto = objectMapper.readValue(msg, BinanceMarkPriceMessage.class);
                monitorEventPublisher.publishPriceTick(dto.getSymbol(), dto.getMarkPrice(), dto.getEventTime());
            }
        } catch (Exception e) {
            log.warn("multiplex message parse failed payload={}", msg, e);
        }
    }
}
