package st.indicator.stindicator.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.exception.CandleFetchFailException;
import st.indicator.stindicator.domain.utils.candle.Candle;
import st.indicator.stindicator.presentation.dto.ChartCandleResponseDto;
import st.indicator.stindicator.presentation.dto.ChartRequestDto;
import st.indicator.stindicator.presentation.dto.ChartResponseDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChartService {
    private static final Logger log = LoggerFactory.getLogger(ChartService.class);
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_PERIOD = 1000;
    private static final int MAX_PERIOD_COUNT = 20;
    private static final int MAX_WARMUP = 500;
    private static final int MAX_FETCH_LIMIT = 1000;
    private static final Set<String> SUPPORTED_INTERVALS = Set.of("1m", "5m", "15m", "1h", "4h", "1d");
    private static final Duration LATEST_TTL = Duration.ofSeconds(20);
    private static final Duration HISTORY_TTL = Duration.ofMinutes(10);
    private static final int MAX_CACHE_ENTRIES = 256;

    private final ExchangeConnector exchangeConnector;
    private final Map<ChartCacheKey, CachedChart> cache = new ConcurrentHashMap<>();

    public ChartService(ExchangeConnector exchangeConnector) {
        this.exchangeConnector = exchangeConnector;
    }

    public ChartResponseDto getChart(ChartRequestDto request) {
        long startedAt = System.nanoTime();
        ChartQuery query = normalize(request);
        ChartCacheKey cacheKey = ChartCacheKey.from(query);
        long now = System.currentTimeMillis();
        CachedChart cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt() > now) {
            return cached.response();
        }

        ChartResponseDto response = load(query, startedAt);
        evictCache(now);
        long ttl = query.direction() == ChartDirection.LATEST ? LATEST_TTL.toMillis() : HISTORY_TTL.toMillis();
        cache.put(cacheKey, new CachedChart(response, now + ttl, now));
        return response;
    }

    private ChartResponseDto load(ChartQuery query, long startedAt) {
        int warmup = warmupCount(query);
        int fetchLimit = Math.min(MAX_FETCH_LIMIT, query.limit() + warmup + 1);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", query.symbol());
        params.put("interval", query.interval());
        params.put("limit", String.valueOf(fetchLimit));
        if (query.direction() == ChartDirection.OLDER) {
            params.put("endTime", String.valueOf(Math.max(0, query.before() - 1)));
        }
        if (query.direction() == ChartDirection.NEWER) {
            long warmupStartTime = Math.max(0, query.after() - (intervalMillis(query.interval()) * warmup));
            params.put("startTime", String.valueOf(warmupStartTime));
        }

        try {
            long fetchStartedAt = System.nanoTime();
            List<Candle> fetched = normalizeCandles(exchangeConnector.getCandles(params));
            long fetchDoneAt = System.nanoTime();
            List<Candle> visibleCandidates = visibleCandidates(fetched, query);
            boolean hasPreviousPage = visibleCandidates.size() > query.limit();
            List<Candle> visible = visibleWindow(visibleCandidates, query);
            long visibleOpenTime = visible.isEmpty() ? Long.MAX_VALUE : visible.getFirst().getOpenTime();
            Map<String, Object> indicators = calculateIndicators(fetched, visibleOpenTime, query);
            long calculatedAt = System.nanoTime();
            Long nextBefore = visible.isEmpty() ? null : visible.getFirst().getOpenTime();
            Long nextAfter = visible.isEmpty() ? null : visible.getLast().getOpenTime();
            boolean hasOlder = hasOlder(query, visible, hasPreviousPage, fetched);
            boolean hasNewer = hasNewer(query, visible, hasPreviousPage);
            log.debug("chart load done symbol={}, interval={}, returned={}, prepared={}, candleFetchMs={}, indicatorMs={}, totalMs={}",
                    query.symbol(), query.interval(), visible.size(), fetched.size(),
                    millis(fetchDoneAt - fetchStartedAt), millis(calculatedAt - fetchDoneAt), millis(calculatedAt - startedAt));
            return new ChartResponseDto(
                    query.symbol(),
                    query.interval(),
                    visible.stream().map(ChartCandleResponseDto::from).toList(),
                    indicators,
                    hasOlder,
                    hasOlder,
                    hasNewer,
                    nextBefore,
                    nextAfter,
                    query.direction().name(),
                    visible.size()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CandleFetchFailException(e, "차트 캔들 조회 실패");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CandleFetchFailException(e, "차트 캔들 조회 실패");
        }
    }

    private List<Candle> visibleCandidates(List<Candle> fetched, ChartQuery query) {
        return switch (query.direction()) {
            case OLDER -> fetched.stream()
                    .filter(candle -> candle.getOpenTime() < query.before())
                    .toList();
            case NEWER -> fetched.stream()
                    .filter(candle -> candle.getOpenTime() > query.after())
                    .toList();
            case LATEST -> fetched;
        };
    }

    private List<Candle> visibleWindow(List<Candle> candidates, ChartQuery query) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (query.direction() == ChartDirection.NEWER) {
            return candidates.subList(0, Math.min(query.limit(), candidates.size()));
        }
        int start = Math.max(0, candidates.size() - query.limit());
        return candidates.subList(start, candidates.size());
    }

    private boolean hasOlder(ChartQuery query, List<Candle> visible, boolean hasPreviousPage, List<Candle> fetched) {
        if (visible.isEmpty()) {
            return false;
        }
        if (query.direction() == ChartDirection.NEWER) {
            long firstVisible = visible.getFirst().getOpenTime();
            return fetched.stream().anyMatch(candle -> candle.getOpenTime() < firstVisible);
        }
        return hasPreviousPage;
    }

    private boolean hasNewer(ChartQuery query, List<Candle> visible, boolean hasNextPage) {
        if (visible.isEmpty()) {
            return false;
        }
        if (query.direction() == ChartDirection.NEWER) {
            return hasNextPage;
        }
        return query.direction() == ChartDirection.OLDER;
    }

    private List<Candle> normalizeCandles(List<Candle> candles) {
        Map<Long, Candle> byOpenTime = new LinkedHashMap<>();
        candles.stream()
                .filter(candle -> candle.getOpenTime() != null)
                .sorted(Comparator.comparing(Candle::getOpenTime))
                .forEach(candle -> byOpenTime.put(candle.getOpenTime(), candle));
        return new ArrayList<>(byOpenTime.values());
    }

    private Map<String, Object> calculateIndicators(List<Candle> candles, long visibleOpenTime, ChartQuery query) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!query.hasIndicators()) {
            return result;
        }
        if (!query.smaPeriods().isEmpty()) {
            List<MovingAverageSeries> smaSeries = query.smaPeriods().stream()
                    .map(period -> new MovingAverageSeries(period, visible(smaPoints(candles, period), visibleOpenTime)))
                    .toList();
            result.put("sma", smaSeries);
        }
        if (!query.emaPeriods().isEmpty()) {
            result.put("ema", query.emaPeriods().stream()
                    .map(period -> new MovingAverageSeries(period, visible(emaPoints(candles, period), visibleOpenTime)))
                    .toList());
        }
        if (query.bollingerEnabled()) {
            List<BollingerPoint> points = bollingerPoints(candles, query.bollingerPeriod(), query.bollingerDeviation());
            result.put("bollingerBands", new BollingerBandsSeries(
                    query.bollingerPeriod(),
                    query.bollingerDeviation(),
                    visible(points, visibleOpenTime)
            ));
        }
        if (query.vwap()) {
            result.put("vwap", new VwapSeries(visible(vwapPoints(candles), visibleOpenTime)));
        }
        return result;
    }

    public List<IndicatorPoint> smaPoints(List<Candle> candles, int period) {
        List<IndicatorPoint> result = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (int index = 0; index < candles.size(); index++) {
            sum = sum.add(candles.get(index).getClose());
            if (index >= period) {
                sum = sum.subtract(candles.get(index - period).getClose());
            }
            if (index >= period - 1) {
                result.add(new IndicatorPoint(
                        candles.get(index).getOpenTime(),
                        sum.divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP)
                ));
            }
        }
        return result;
    }

    public List<IndicatorPoint> emaPoints(List<Candle> candles, int period) {
        List<IndicatorPoint> result = new ArrayList<>();
        if (candles.size() < period) {
            return result;
        }
        BigDecimal seed = BigDecimal.ZERO;
        for (int index = 0; index < period; index++) {
            seed = seed.add(candles.get(index).getClose());
        }
        seed = seed.divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP);
        result.add(new IndicatorPoint(candles.get(period - 1).getOpenTime(), seed));
        BigDecimal alpha = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1L), 18, RoundingMode.HALF_UP);
        BigDecimal previous = seed;
        for (int index = period; index < candles.size(); index++) {
            previous = candles.get(index).getClose().multiply(alpha)
                    .add(previous.multiply(BigDecimal.ONE.subtract(alpha)));
            result.add(new IndicatorPoint(candles.get(index).getOpenTime(), previous));
        }
        return result;
    }

    public List<BollingerPoint> bollingerPoints(List<Candle> candles, int period, BigDecimal deviation) {
        List<BollingerPoint> result = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal sumSquares = BigDecimal.ZERO;
        for (int index = 0; index < candles.size(); index++) {
            BigDecimal close = candles.get(index).getClose();
            sum = sum.add(close);
            sumSquares = sumSquares.add(close.multiply(close));
            if (index >= period) {
                BigDecimal old = candles.get(index - period).getClose();
                sum = sum.subtract(old);
                sumSquares = sumSquares.subtract(old.multiply(old));
            }
            if (index >= period - 1) {
                BigDecimal periodDecimal = BigDecimal.valueOf(period);
                BigDecimal mean = sum.divide(periodDecimal, 18, RoundingMode.HALF_UP);
                BigDecimal variance = sumSquares.divide(periodDecimal, 18, RoundingMode.HALF_UP).subtract(mean.multiply(mean));
                BigDecimal standardDeviation = sqrt(variance.max(BigDecimal.ZERO));
                BigDecimal bandDistance = standardDeviation.multiply(deviation);
                result.add(new BollingerPoint(
                        candles.get(index).getOpenTime(),
                        mean,
                        mean.add(bandDistance),
                        mean.subtract(bandDistance)
                ));
            }
        }
        return result;
    }

    public List<IndicatorPoint> vwapPoints(List<Candle> candles) {
        List<IndicatorPoint> result = new ArrayList<>();
        BigDecimal cumulativePriceVolume = BigDecimal.ZERO;
        BigDecimal cumulativeVolume = BigDecimal.ZERO;
        String currentSession = null;
        for (Candle candle : candles) {
            String session = Instant.ofEpochMilli(candle.getOpenTime()).atZone(ZoneOffset.UTC).toLocalDate().toString();
            if (!session.equals(currentSession)) {
                currentSession = session;
                cumulativePriceVolume = BigDecimal.ZERO;
                cumulativeVolume = BigDecimal.ZERO;
            }
            BigDecimal volume = candle.getVolume();
            if (volume == null || volume.signum() <= 0) {
                continue;
            }
            BigDecimal typicalPrice = candle.getHigh().add(candle.getLow()).add(candle.getClose())
                    .divide(BigDecimal.valueOf(3), 18, RoundingMode.HALF_UP);
            cumulativePriceVolume = cumulativePriceVolume.add(typicalPrice.multiply(volume));
            cumulativeVolume = cumulativeVolume.add(volume);
            if (cumulativeVolume.signum() > 0) {
                result.add(new IndicatorPoint(
                        candle.getOpenTime(),
                        cumulativePriceVolume.divide(cumulativeVolume, 18, RoundingMode.HALF_UP)
                ));
            }
        }
        return result;
    }

    private BigDecimal sqrt(BigDecimal value) {
        if (value.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(Math.sqrt(value.doubleValue()));
    }

    private <T extends TimedPoint> List<T> visible(List<T> values, long visibleOpenTime) {
        if (values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value.time() >= visibleOpenTime)
                .toList();
    }

    private int warmupCount(ChartQuery query) {
        int warmup = 0;
        for (Integer period : query.smaPeriods()) {
            warmup = Math.max(warmup, period - 1);
        }
        if (query.bollingerEnabled()) {
            warmup = Math.max(warmup, query.bollingerPeriod() - 1);
        }
        for (Integer period : query.emaPeriods()) {
            warmup = Math.max(warmup, Math.max(period * 3, period + 100));
        }
        if (query.vwap()) {
            long intervalMillis = intervalMillis(query.interval());
            int sessionWarmup = intervalMillis <= 0 ? 0 : (int) Math.min(MAX_WARMUP, Duration.ofDays(1).toMillis() / intervalMillis);
            warmup = Math.max(warmup, sessionWarmup);
        }
        return Math.min(MAX_WARMUP, Math.max(0, warmup));
    }

    private long intervalMillis(String interval) {
        return switch (interval) {
            case "1m" -> Duration.ofMinutes(1).toMillis();
            case "5m" -> Duration.ofMinutes(5).toMillis();
            case "15m" -> Duration.ofMinutes(15).toMillis();
            case "1h" -> Duration.ofHours(1).toMillis();
            case "4h" -> Duration.ofHours(4).toMillis();
            case "1d" -> Duration.ofDays(1).toMillis();
            default -> Duration.ofHours(1).toMillis();
        };
    }

    private ChartQuery normalize(ChartRequestDto request) {
        if (request == null || request.getSymbol() == null || request.getSymbol().isBlank()) {
            throw new IllegalArgumentException("symbol은 필수입니다.");
        }
        String symbol = request.getSymbol().trim().toUpperCase(Locale.ROOT);
        if (!symbol.matches("[A-Z0-9]{2,20}USDT")) {
            throw new IllegalArgumentException("지원하지 않는 USDT 선물 심볼 형식입니다.");
        }
        String interval = request.getInterval() == null ? "1h" : request.getInterval().trim();
        if (!SUPPORTED_INTERVALS.contains(interval)) {
            throw new IllegalArgumentException("지원 interval은 1m, 5m, 15m, 1h, 4h, 1d입니다.");
        }
        int limit = request.getLimit() == null ? DEFAULT_LIMIT : request.getLimit();
        if (limit <= 0) {
            throw new IllegalArgumentException("limit은 1 이상이어야 합니다.");
        }
        limit = Math.min(limit, MAX_LIMIT);
        Long before = request.getBefore() == null ? request.getEndTime() : request.getBefore();
        Long after = request.getAfter();
        if (before != null && after != null) {
            throw new IllegalArgumentException("before와 after는 동시에 사용할 수 없습니다.");
        }
        if (before != null && before < 0) {
            throw new IllegalArgumentException("before는 0 이상이어야 합니다.");
        }
        if (after != null && after < 0) {
            throw new IllegalArgumentException("after는 0 이상이어야 합니다.");
        }
        ChartDirection direction = before != null ? ChartDirection.OLDER : after != null ? ChartDirection.NEWER : ChartDirection.LATEST;
        List<Integer> emaPeriods = periods(request.getEmaPeriods(), request.getIndicators(), "EMA", request.getEmaPeriod(), "emaPeriods");
        List<Integer> smaPeriods = periods(request.getSmaPeriods(), request.getIndicators(), "SMA", request.getSmaPeriod(), "smaPeriods");
        Integer bollingerPeriod = request.getBollingerPeriod();
        boolean bollingerEnabled = bollingerPeriod != null;
        if (bollingerEnabled) {
            validatePeriod(bollingerPeriod, "bollingerPeriod");
        } else {
            bollingerPeriod = 20;
        }
        BigDecimal bollingerDeviation = request.getBollingerDeviation() == null ? BigDecimal.valueOf(2) : request.getBollingerDeviation();
        if (bollingerDeviation.compareTo(new BigDecimal("0.1")) < 0 || bollingerDeviation.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("bollingerDeviation은 0.1 이상 10 이하의 숫자여야 합니다.");
        }
        return new ChartQuery(
                symbol,
                interval,
                limit,
                before,
                after,
                direction,
                emaPeriods,
                smaPeriods,
                bollingerEnabled,
                bollingerPeriod,
                bollingerDeviation,
                Boolean.TRUE.equals(request.getVwap())
        );
    }

    private List<Integer> periods(String csv, String legacyIndicators, String legacyName, Integer legacyPeriod, String fieldName) {
        LinkedHashSet<Integer> values = new LinkedHashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String item : csv.split(",")) {
                try {
                    int period = Integer.parseInt(item.trim());
                    validatePeriod(period, fieldName);
                    values.add(period);
                } catch (NumberFormatException error) {
                    throw new IllegalArgumentException(fieldName + "는 정수 CSV여야 합니다.");
                }
            }
        } else if (legacyIndicators != null && legacyIndicators.toUpperCase(Locale.ROOT).contains(legacyName)) {
            int period = legacyPeriod == null ? 20 : legacyPeriod;
            validatePeriod(period, fieldName);
            values.add(period);
        }
        if (values.size() > MAX_PERIOD_COUNT) {
            throw new IllegalArgumentException(fieldName + "는 최대 " + MAX_PERIOD_COUNT + "개까지 요청할 수 있습니다.");
        }
        return List.copyOf(values);
    }

    private void validatePeriod(int period, String fieldName) {
        if (period < 1 || period > MAX_PERIOD) {
            throw new IllegalArgumentException(fieldName + "는 1 이상 1000 이하의 정수여야 합니다.");
        }
    }

    private void evictCache(long now) {
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (cache.size() < MAX_CACHE_ENTRIES) {
            return;
        }
        cache.entrySet().stream()
                .min(Comparator.comparingLong(entry -> entry.getValue().createdAt()))
                .ifPresent(entry -> cache.remove(entry.getKey()));
    }

    private double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    public interface TimedPoint {
        Long time();
    }

    public record IndicatorPoint(Long time, BigDecimal value) implements TimedPoint {
    }

    public record MovingAverageSeries(int period, List<IndicatorPoint> points) {
    }

    public record BollingerPoint(Long time, BigDecimal middle, BigDecimal upper, BigDecimal lower) implements TimedPoint {
    }

    public record BollingerBandsSeries(int period, BigDecimal deviation, List<BollingerPoint> points) {
    }

    public record VwapSeries(List<IndicatorPoint> points) {
    }

    private enum ChartDirection {
        LATEST,
        OLDER,
        NEWER
    }

    private record ChartQuery(
            String symbol,
            String interval,
            int limit,
            Long before,
            Long after,
            ChartDirection direction,
            List<Integer> emaPeriods,
            List<Integer> smaPeriods,
            boolean bollingerEnabled,
            int bollingerPeriod,
            BigDecimal bollingerDeviation,
            boolean vwap
    ) {
        private boolean hasIndicators() {
            return !emaPeriods.isEmpty() || !smaPeriods.isEmpty() || bollingerEnabled || vwap;
        }
    }

    private record ChartCacheKey(
            String symbol,
            String interval,
            int limit,
            Long before,
            Long after,
            ChartDirection direction,
            List<Integer> emaPeriods,
            List<Integer> smaPeriods,
            boolean bollingerEnabled,
            int bollingerPeriod,
            String bollingerDeviation,
            boolean vwap
    ) {
        private static ChartCacheKey from(ChartQuery query) {
            return new ChartCacheKey(
                    query.symbol(), query.interval(), query.limit(), query.before(), query.after(), query.direction(),
                    query.emaPeriods(), query.smaPeriods(), query.bollingerEnabled(),
                    query.bollingerPeriod(), query.bollingerDeviation().stripTrailingZeros().toPlainString(), query.vwap()
            );
        }
    }

    private record CachedChart(ChartResponseDto response, long expiresAt, long createdAt) {
    }
}
