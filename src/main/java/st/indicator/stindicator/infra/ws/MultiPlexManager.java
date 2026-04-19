package st.indicator.stindicator.infra.ws;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.websocket.WebsocketOutbound;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MultiPlexManager {

    private static final String WS_URL = "wss://fstream.binance.com/ws";
    private final MonitorService monitorService;

    private final ObjectMapper objectMapper;

    // 현재 구독 중인 stream 목록
    private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();

    private volatile WebsocketOutbound outbound;
    private volatile Disposable connection;

    public MultiPlexManager(MonitorService monitorService, ObjectMapper objectMapper) {
        this.monitorService = monitorService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        connect();
        subscribe("btcusdt@kline_1m");
        subscribe("ethusdt@kline_1m");
    }

    private void connect() {
        this.connection = HttpClient.create()
                .websocket()
                .uri(WS_URL)
                .handle((in, out) -> {
                    this.outbound = out;

                    // 재연결 시 기존 구독 복구
                    resendSubscriptions();

                    return in.receive()
                            .asString()
                            .doOnNext(this::handleMessage)
                            .doOnError(e -> reconnect())
                            .then();
                })
                .retryWhen(reactor.util.retry.Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(3)))
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
            sendMessage(buildSubscribeMessage(stream));
        }
    }

    //구독 취소
    public void unsubscribe(String stream) {
        if (subscriptions.remove(stream)) {
            sendMessage(buildUnsubscribeMessage(stream));
        }
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
//        System.out.println(msg);

        try {
            JsonNode node = objectMapper.readTree(msg);

            if (node.has("result")) {
                return;
            }

            KlineEventDTO dto = objectMapper.readValue(msg, KlineEventDTO.class);

            monitorService.push(dto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}