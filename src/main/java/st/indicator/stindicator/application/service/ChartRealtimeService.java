package st.indicator.stindicator.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import st.indicator.stindicator.domain.utils.candle.Candle;
import st.indicator.stindicator.infra.ws.dto.binance.KlineData;
import st.indicator.stindicator.infra.ws.dto.binance.KlineEventDTO;
import st.indicator.stindicator.presentation.dto.SymbolMonitorDto;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class ChartRealtimeService {
    private static final Logger log = LoggerFactory.getLogger(ChartRealtimeService.class);
    private static final Set<String> SUPPORTED_INTERVALS = Set.of("1m", "5m", "15m", "1h", "4h", "1d");
    private static final int SEED_LIMIT = 1000;
    private static final int MAX_ROLLING_CANDLES = 1000;
    private static final int MAX_PERIOD = 1000;
    private static final int MAX_PERIOD_COUNT = 20;
    private static final int MAX_CHART_SUBSCRIPTIONS_PER_SESSION = 8;

    private final ExchangeConnector exchangeConnector;
    private final ChartService chartService;
    private final ObjectMapper objectMapper;
    private final Map<String, ChartSubscription> subscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> streamSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> sessionSendLocks = new ConcurrentHashMap<>();
    private final Map<String, RollingChartState> states = new ConcurrentHashMap<>();

    public ChartRealtimeService(ExchangeConnector exchangeConnector, ChartService chartService, ObjectMapper objectMapper) {
        this.exchangeConnector = exchangeConnector;
        this.chartService = chartService;
        this.objectMapper = objectMapper;
    }

    public Set<String> subscribe(WebSocketSession session, SymbolMonitorDto request) {
        String symbol = firstSymbol(request).toUpperCase();
        String interval = normalizeInterval(request.getInterval());
        String streamKey = toKlineStreamKey(symbol, interval);
        ChartIndicatorConfig config = ChartIndicatorConfig.from(request);
        String subscriptionId = request.getSubscriptionId() == null || request.getSubscriptionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSubscriptionId();
        long configVersion = request.getConfigVersion() == null ? 1L : request.getConfigVersion();

        unsubscribe(session, request);
        if (sessionSubscriptions.getOrDefault(session.getId(), Set.of()).size() >= MAX_CHART_SUBSCRIPTIONS_PER_SESSION) {
            throw new IllegalArgumentException("chart subscription limit exceeded");
        }
        RollingChartState state = states.computeIfAbsent(streamKey, ignored -> seedState(symbol, interval));
        ChartSubscription subscription = new ChartSubscription(
                subscriptionId,
                session,
                symbol,
                interval,
                streamKey,
                configVersion,
                config,
                new AtomicLong()
        );
        subscriptions.put(subscriptionId, subscription);
        streamSubscriptions.computeIfAbsent(streamKey, ignored -> ConcurrentHashMap.newKeySet()).add(subscriptionId);
        sessionSubscriptions.computeIfAbsent(session.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(subscriptionId);
        log.info("chart realtime subscribed session={}, subscriptionId={}, stream={}, config={}",
                session.getId(), subscriptionId, streamKey, config);
        sendSubscriptionStatus(subscription, "CHART_SUBSCRIBED", state.lastOpenTime());
        return Set.of(streamKey);
    }

    public Set<String> unsubscribe(WebSocketSession session, SymbolMonitorDto request) {
        Set<String> releasedStreams = new HashSet<>();
        String requestedSubscriptionId = request.getSubscriptionId();
        Set<String> targets = new HashSet<>();
        if (requestedSubscriptionId != null && !requestedSubscriptionId.isBlank()) {
            targets.add(requestedSubscriptionId);
        } else {
            String symbol = request.getSymbols() == null || request.getSymbols().isEmpty() ? null : request.getSymbols().getFirst();
            String interval = request.getInterval();
            Set<String> sessionIds = sessionSubscriptions.getOrDefault(session.getId(), Set.of());
            sessionIds.stream()
                    .map(subscriptions::get)
                    .filter(subscription -> subscription != null)
                    .filter(subscription -> symbol == null || subscription.symbol().equalsIgnoreCase(symbol))
                    .filter(subscription -> interval == null || subscription.interval().equalsIgnoreCase(interval))
                    .map(ChartSubscription::subscriptionId)
                    .forEach(targets::add);
        }
        targets.forEach(subscriptionId -> removeSubscription(session.getId(), subscriptionId, releasedStreams));
        return releasedStreams;
    }

    public Set<String> unsubscribe(WebSocketSession session) {
        Set<String> releasedStreams = new HashSet<>();
        Set<String> subscriptionIds = new HashSet<>(sessionSubscriptions.getOrDefault(session.getId(), Set.of()));
        subscriptionIds.forEach(subscriptionId -> removeSubscription(session.getId(), subscriptionId, releasedStreams));
        sessionSendLocks.remove(session.getId());
        return releasedStreams;
    }

    public boolean hasStreamSubscribers(String streamKey) {
        Set<String> subscriptionIds = streamSubscriptions.get(streamKey);
        if (subscriptionIds == null) {
            return false;
        }
        subscriptionIds.removeIf(subscriptionId -> {
            ChartSubscription subscription = subscriptions.get(subscriptionId);
            return subscription == null || !subscription.session().isOpen();
        });
        if (subscriptionIds.isEmpty()) {
            streamSubscriptions.remove(streamKey);
            states.remove(streamKey);
            return false;
        }
        return true;
    }

    public void handleKline(KlineEventDTO event) {
        String streamKey = toKlineStreamKey(event.getSymbol(), event.getKline().getInterval());
        Set<String> subscriptionIds = streamSubscriptions.get(streamKey);
        if (subscriptionIds == null || subscriptionIds.isEmpty()) {
            return;
        }
        RollingChartState state = states.computeIfAbsent(streamKey, ignored -> seedState(event.getSymbol(), event.getKline().getInterval()));
        backfillMissingCandlesIfNeeded(streamKey, state, event);
        CandleUpdate update = state.apply(event);
        if (!update.applied()) {
            log.debug("chart realtime kline skipped stream={}, reason={}, openTime={}", streamKey, update.reason(), event.getKline().getOpenTime());
            return;
        }
        subscriptionIds.removeIf(subscriptionId -> {
            ChartSubscription subscription = subscriptions.get(subscriptionId);
            if (subscription == null || !subscription.session().isOpen()) {
                subscriptions.remove(subscriptionId);
                return true;
            }
            sendUpdate(subscription, update.candle(), event.getKline().isClosed(), state);
            return false;
        });
        if (subscriptionIds.isEmpty()) {
            streamSubscriptions.remove(streamKey);
            states.remove(streamKey);
        }
    }

    private void removeSubscription(String sessionId, String subscriptionId, Set<String> releasedStreams) {
        ChartSubscription subscription = subscriptions.remove(subscriptionId);
        if (subscription == null) {
            return;
        }
        Set<String> sessionIds = sessionSubscriptions.get(sessionId);
        if (sessionIds != null) {
            sessionIds.remove(subscriptionId);
            if (sessionIds.isEmpty()) {
                sessionSubscriptions.remove(sessionId);
            }
        }
        Set<String> streamIds = streamSubscriptions.get(subscription.streamKey());
        if (streamIds != null) {
            streamIds.remove(subscriptionId);
            if (streamIds.isEmpty()) {
                streamSubscriptions.remove(subscription.streamKey());
                states.remove(subscription.streamKey());
                releasedStreams.add(subscription.streamKey());
            }
        }
        log.info("chart realtime unsubscribed session={}, subscriptionId={}, stream={}",
                sessionId, subscriptionId, subscription.streamKey());
    }

    private RollingChartState seedState(String symbol, String interval) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol.toUpperCase());
        params.put("interval", normalizeInterval(interval));
        params.put("limit", String.valueOf(SEED_LIMIT));
        try {
            List<Candle> candles = new ArrayList<>(exchangeConnector.getCandles(params));
            candles.removeIf(candle -> candle.getOpenTime() == null);
            candles.sort(Comparator.comparing(Candle::getOpenTime));
            return new RollingChartState(candles);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("chart realtime seed failed symbol={}, interval={}", symbol, interval, error);
            return new RollingChartState(List.of());
        }
    }

    private void backfillMissingCandlesIfNeeded(String streamKey, RollingChartState state, KlineEventDTO event) {
        long intervalMillis = intervalMillis(event.getKline().getInterval());
        Long lastOpenTime = state.lastOpenTime();
        long currentOpenTime = event.getKline().getOpenTime();
        if (lastOpenTime == null || intervalMillis <= 0 || currentOpenTime <= lastOpenTime + intervalMillis) {
            return;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", event.getSymbol().toUpperCase());
        params.put("interval", normalizeInterval(event.getKline().getInterval()));
        params.put("startTime", String.valueOf(lastOpenTime + intervalMillis));
        params.put("endTime", String.valueOf(currentOpenTime - 1));
        params.put("limit", String.valueOf(SEED_LIMIT));
        try {
            List<Candle> missing = new ArrayList<>(exchangeConnector.getCandles(params));
            state.mergeBackfill(missing);
            log.info("chart realtime backfilled stream={}, from={}, to={}, count={}",
                    streamKey, lastOpenTime + intervalMillis, currentOpenTime - 1, missing.size());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("chart realtime backfill failed stream={}, from={}, to={}",
                    streamKey, lastOpenTime + intervalMillis, currentOpenTime - 1, error);
        }
    }

    private void sendSubscriptionStatus(ChartSubscription subscription, String eventType, Long lastOpenTime) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", eventType);
        payload.put("type", eventType);
        payload.put("subscriptionId", subscription.subscriptionId());
        payload.put("symbol", subscription.symbol());
        payload.put("interval", subscription.interval());
        payload.put("configVersion", subscription.configVersion());
        payload.put("lastOpenTime", lastOpenTime);
        send(subscription, payload, eventType);
    }

    private void sendUpdate(ChartSubscription subscription, Candle candle, boolean closed, RollingChartState state) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventType", "CHART_REALTIME_UPDATE");
        payload.put("type", "CHART_REALTIME_UPDATE");
        payload.put("subscriptionId", subscription.subscriptionId());
        payload.put("symbol", subscription.symbol());
        payload.put("interval", subscription.interval());
        payload.put("configVersion", subscription.configVersion());
        payload.put("sequence", subscription.sequence().incrementAndGet());
        payload.put("candle", Map.of(
                "openTime", candle.getOpenTime(),
                "closeTime", candle.getCloseTime(),
                "open", candle.getOpen().toPlainString(),
                "high", candle.getHigh().toPlainString(),
                "low", candle.getLow().toPlainString(),
                "close", candle.getClose().toPlainString(),
                "volume", candle.getVolume().toPlainString(),
                "closed", closed
        ));
        payload.put("indicators", indicators(subscription.config(), candle.getOpenTime(), state.candles()));
        send(subscription, payload, "chartRealtimeUpdate");
    }

    private Map<String, Object> indicators(ChartIndicatorConfig config, long openTime, List<Candle> candles) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!config.emaPeriods().isEmpty()) {
            List<Map<String, Object>> ema = config.emaPeriods().stream()
                    .map(period -> latestPoint(chartService.emaPoints(candles, period), period, openTime))
                    .filter(point -> !point.isEmpty())
                    .toList();
            if (!ema.isEmpty()) {
                result.put("ema", ema);
            }
        }
        if (!config.smaPeriods().isEmpty()) {
            List<Map<String, Object>> sma = config.smaPeriods().stream()
                    .map(period -> latestPoint(chartService.smaPoints(candles, period), period, openTime))
                    .filter(point -> !point.isEmpty())
                    .toList();
            if (!sma.isEmpty()) {
                result.put("sma", sma);
            }
        }
        if (config.bollingerPeriod() != null) {
            chartService.bollingerPoints(candles, config.bollingerPeriod(), config.bollingerDeviation())
                    .stream()
                    .filter(point -> point.time() == openTime)
                    .reduce((first, second) -> second)
                    .ifPresent(point -> result.put("bollingerBands", Map.of(
                            "period", config.bollingerPeriod(),
                            "deviation", config.bollingerDeviation().toPlainString(),
                            "time", point.time(),
                            "middle", point.middle().toPlainString(),
                            "upper", point.upper().toPlainString(),
                            "lower", point.lower().toPlainString()
                    )));
        }
        if (config.vwap()) {
            chartService.vwapPoints(candles)
                    .stream()
                    .filter(point -> point.time() == openTime)
                    .reduce((first, second) -> second)
                    .ifPresent(point -> result.put("vwap", Map.of(
                            "time", point.time(),
                            "value", point.value().toPlainString()
                    )));
        }
        return result;
    }

    private Map<String, Object> latestPoint(List<ChartService.IndicatorPoint> points, int period, long openTime) {
        return points.stream()
                .filter(point -> point.time() == openTime)
                .reduce((first, second) -> second)
                .<Map<String, Object>>map(point -> Map.of(
                        "period", period,
                        "time", point.time(),
                        "value", point.value().toPlainString()
                ))
                .orElse(Map.of());
    }

    private void send(ChartSubscription subscription, Map<String, Object> payload, String eventName) {
        WebSocketSession session = subscription.session();
        if (!session.isOpen()) {
            return;
        }
        ReentrantLock lock = sessionSendLocks.computeIfAbsent(session.getId(), ignored -> new ReentrantLock());
        lock.lock();
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (Exception error) {
            log.warn("chart realtime send failed event={}, session={}, subscriptionId={}",
                    eventName, session.getId(), subscription.subscriptionId(), error);
        } finally {
            lock.unlock();
        }
    }

    private String firstSymbol(SymbolMonitorDto request) {
        if (request.getSymbols() == null || request.getSymbols().isEmpty() || request.getSymbols().getFirst() == null) {
            throw new IllegalArgumentException("chart subscription symbol is required");
        }
        String symbol = request.getSymbols().getFirst().trim().toUpperCase();
        if (!symbol.matches("[A-Z0-9]{2,20}USDT")) {
            throw new IllegalArgumentException("unsupported chart symbol: " + symbol);
        }
        return symbol;
    }

    private String normalizeInterval(String interval) {
        String normalized = interval == null || interval.isBlank() ? "1m" : interval.toLowerCase();
        if (!SUPPORTED_INTERVALS.contains(normalized)) {
            throw new IllegalArgumentException("unsupported chart interval: " + normalized);
        }
        return normalized;
    }

    private String toKlineStreamKey(String symbol, String interval) {
        return symbol.toLowerCase() + "@kline_" + normalizeInterval(interval);
    }

    private long intervalMillis(String interval) {
        return switch (normalizeInterval(interval)) {
            case "1m" -> 60_000L;
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "1h" -> 3_600_000L;
            case "4h" -> 14_400_000L;
            case "1d" -> 86_400_000L;
            default -> 0L;
        };
    }

    private record ChartSubscription(
            String subscriptionId,
            WebSocketSession session,
            String symbol,
            String interval,
            String streamKey,
            long configVersion,
            ChartIndicatorConfig config,
            AtomicLong sequence
    ) {
    }

    private record ChartIndicatorConfig(
            List<Integer> emaPeriods,
            List<Integer> smaPeriods,
            Integer bollingerPeriod,
            BigDecimal bollingerDeviation,
            boolean vwap
    ) {
        private static ChartIndicatorConfig from(SymbolMonitorDto request) {
            Integer bollingerPeriod = request.getBollingerPeriod();
            if (bollingerPeriod != null) {
                validatePeriod(bollingerPeriod, "bollingerPeriod");
            }
            BigDecimal deviation = request.getBollingerDeviation() == null ? BigDecimal.valueOf(2) : request.getBollingerDeviation();
            if (deviation.compareTo(new BigDecimal("0.1")) < 0 || deviation.compareTo(BigDecimal.TEN) > 0) {
                throw new IllegalArgumentException("bollingerDeviation must be between 0.1 and 10");
            }
            return new ChartIndicatorConfig(
                    normalizePeriods(request.getEmaPeriods()),
                    normalizePeriods(request.getSmaPeriods()),
                    bollingerPeriod,
                    deviation,
                    Boolean.TRUE.equals(request.getVwap())
            );
        }

        private static List<Integer> normalizePeriods(List<Integer> periods) {
            if (periods == null) {
                return List.of();
            }
            LinkedHashSet<Integer> result = new LinkedHashSet<>();
            for (Integer period : periods) {
                validatePeriod(period, "period");
                result.add(period);
            }
            if (result.size() > MAX_PERIOD_COUNT) {
                throw new IllegalArgumentException("indicator periods can contain up to " + MAX_PERIOD_COUNT + " values");
            }
            return result.stream().sorted().toList();
        }

        private static void validatePeriod(Integer period, String fieldName) {
            if (period == null || period < 1 || period > MAX_PERIOD) {
                throw new IllegalArgumentException(fieldName + " must be between 1 and " + MAX_PERIOD);
            }
        }
    }

    private static final class RollingChartState {
        private final List<Candle> candles = new ArrayList<>();
        private final Map<Long, Boolean> closedByOpenTime = new ConcurrentHashMap<>();
        private final Map<Long, Long> eventTimeByOpenTime = new ConcurrentHashMap<>();

        private RollingChartState(List<Candle> seedCandles) {
            candles.addAll(seedCandles);
            trim();
        }

        private synchronized CandleUpdate apply(KlineEventDTO event) {
            KlineData data = event.getKline();
            long openTime = data.getOpenTime();
            Boolean alreadyClosed = closedByOpenTime.get(openTime);
            if (Boolean.TRUE.equals(alreadyClosed) && !data.isClosed()) {
                return CandleUpdate.skipped("CLOSED_TO_OPEN_REJECTED");
            }
            Long previousEventTime = eventTimeByOpenTime.get(openTime);
            if (previousEventTime != null && event.getEventTime() < previousEventTime) {
                return CandleUpdate.skipped("STALE_EVENT_TIME");
            }
            Candle candle = new Candle(
                    openTime,
                    decimal(data.getOpenPrice()),
                    decimal(data.getHighPrice()),
                    decimal(data.getLowPrice()),
                    decimal(data.getClosePrice()),
                    decimal(data.getVolume()),
                    data.getCloseTime()
            );
            int index = indexOf(openTime);
            if (index >= 0) {
                candles.set(index, candle);
            } else {
                candles.add(candle);
                candles.sort(Comparator.comparing(Candle::getOpenTime));
            }
            closedByOpenTime.put(openTime, data.isClosed());
            eventTimeByOpenTime.put(openTime, event.getEventTime());
            trim();
            return CandleUpdate.applied(candle);
        }

        private synchronized List<Candle> candles() {
            return List.copyOf(candles);
        }

        private synchronized Long lastOpenTime() {
            return candles.isEmpty() ? null : candles.getLast().getOpenTime();
        }

        private synchronized void mergeBackfill(List<Candle> missing) {
            missing.stream()
                    .filter(candle -> candle.getOpenTime() != null)
                    .forEach(candle -> {
                        int index = indexOf(candle.getOpenTime());
                        if (index >= 0) {
                            candles.set(index, candle);
                        } else {
                            candles.add(candle);
                        }
                        closedByOpenTime.put(candle.getOpenTime(), true);
                    });
            candles.sort(Comparator.comparing(Candle::getOpenTime));
            trim();
        }

        private int indexOf(long openTime) {
            for (int index = 0; index < candles.size(); index++) {
                if (candles.get(index).getOpenTime() == openTime) {
                    return index;
                }
            }
            return -1;
        }

        private void trim() {
            while (candles.size() > MAX_ROLLING_CANDLES) {
                Candle removed = candles.removeFirst();
                closedByOpenTime.remove(removed.getOpenTime());
                eventTimeByOpenTime.remove(removed.getOpenTime());
            }
        }

        private static BigDecimal decimal(String value) {
            return value == null || value.isBlank() ? BigDecimal.ZERO : new BigDecimal(value);
        }
    }

    private record CandleUpdate(boolean applied, Candle candle, String reason) {
        private static CandleUpdate applied(Candle candle) {
            return new CandleUpdate(true, candle, null);
        }

        private static CandleUpdate skipped(String reason) {
            return new CandleUpdate(false, null, reason);
        }
    }
}
