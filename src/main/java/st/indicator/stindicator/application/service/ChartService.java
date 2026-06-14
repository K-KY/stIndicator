package st.indicator.stindicator.application.service;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChartService {
    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 500;
    private static final int MAX_WARMUP = 100;
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
        ChartQuery query = normalize(request);
        ChartCacheKey cacheKey = ChartCacheKey.from(query);
        long now = System.currentTimeMillis();
        CachedChart cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt() > now) {
            return cached.response();
        }

        ChartResponseDto response = load(query);
        evictCache(now);
        long ttl = query.endTime() == null ? LATEST_TTL.toMillis() : HISTORY_TTL.toMillis();
        cache.put(cacheKey, new CachedChart(response, now + ttl, now));
        return response;
    }

    private ChartResponseDto load(ChartQuery query) {
        int warmup = warmupCount(query);
        int fetchLimit = Math.min(MAX_LIMIT + MAX_WARMUP + 1, query.limit() + warmup + 1);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", query.symbol());
        params.put("interval", query.interval());
        params.put("limit", String.valueOf(fetchLimit));
        if (query.endTime() != null) {
            params.put("endTime", String.valueOf(Math.max(0, query.endTime() - 1)));
        }

        try {
            List<Candle> fetched = new ArrayList<>(exchangeConnector.getCandles(params));
            fetched.sort(Comparator.comparing(Candle::getOpenTime));
            int visibleStart = Math.max(0, fetched.size() - query.limit());
            List<Candle> visible = fetched.subList(visibleStart, fetched.size());
            Map<String, Object> indicators = calculateIndicators(fetched, visibleStart, query);
            Long nextBefore = visible.isEmpty() ? null : visible.getFirst().getOpenTime();
            boolean hasMore = !visible.isEmpty() && fetched.size() > query.limit();
            return new ChartResponseDto(
                    query.symbol(),
                    query.interval(),
                    visible.stream().map(ChartCandleResponseDto::from).toList(),
                    indicators,
                    hasMore,
                    nextBefore
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CandleFetchFailException(e, "차트 캔들 조회 실패");
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new CandleFetchFailException(e, "차트 캔들 조회 실패");
        }
    }

    private Map<String, Object> calculateIndicators(List<Candle> candles, int visibleStart, ChartQuery query) {
        if (query.indicators().isEmpty()) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        List<BigDecimal> closes = candles.stream().map(Candle::getClose).toList();
        if (query.indicators().contains(Indicator.SMA)) {
            result.put("SMA", visible(sma(closes, query.smaPeriod()), visibleStart));
        }
        if (query.indicators().contains(Indicator.EMA)) {
            result.put("EMA", visible(ema(closes, query.emaPeriod()), visibleStart));
        }
        if (query.indicators().contains(Indicator.RSI)) {
            result.put("RSI", visible(rsi(closes, query.rsiPeriod()), visibleStart));
        }
        if (query.indicators().contains(Indicator.MACD)) {
            List<BigDecimal> fast = ema(closes, query.macdFastPeriod());
            List<BigDecimal> slow = ema(closes, query.macdSlowPeriod());
            List<BigDecimal> macd = subtract(fast, slow);
            List<BigDecimal> signal = emaNullable(macd, query.macdSignalPeriod());
            Map<String, Object> macdResult = new LinkedHashMap<>();
            macdResult.put("macd", visible(macd, visibleStart));
            macdResult.put("signal", visible(signal, visibleStart));
            macdResult.put("histogram", visible(subtract(macd, signal), visibleStart));
            result.put("MACD", macdResult);
        }
        return result;
    }

    private List<BigDecimal> sma(List<BigDecimal> values, int period) {
        List<BigDecimal> result = nullList(values.size());
        BigDecimal sum = BigDecimal.ZERO;
        for (int index = 0; index < values.size(); index++) {
            sum = sum.add(values.get(index));
            if (index >= period) {
                sum = sum.subtract(values.get(index - period));
            }
            if (index >= period - 1) {
                result.set(index, sum.divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP));
            }
        }
        return result;
    }

    private List<BigDecimal> ema(List<BigDecimal> values, int period) {
        List<BigDecimal> result = nullList(values.size());
        if (values.size() < period) {
            return result;
        }
        BigDecimal seed = values.subList(0, period).stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(period), 12, RoundingMode.HALF_UP);
        result.set(period - 1, seed);
        BigDecimal multiplier = BigDecimal.valueOf(2)
                .divide(BigDecimal.valueOf(period + 1L), 18, RoundingMode.HALF_UP);
        BigDecimal previous = seed;
        for (int index = period; index < values.size(); index++) {
            previous = values.get(index).subtract(previous).multiply(multiplier).add(previous);
            result.set(index, previous);
        }
        return result;
    }

    private List<BigDecimal> emaNullable(List<BigDecimal> values, int period) {
        List<BigDecimal> result = nullList(values.size());
        int first = 0;
        while (first < values.size() && values.get(first) == null) {
            first++;
        }
        if (values.size() - first < period) {
            return result;
        }
        List<BigDecimal> compact = values.subList(first, values.size());
        List<BigDecimal> calculated = ema(compact, period);
        for (int index = 0; index < calculated.size(); index++) {
            result.set(first + index, calculated.get(index));
        }
        return result;
    }

    private List<BigDecimal> rsi(List<BigDecimal> values, int period) {
        List<BigDecimal> result = nullList(values.size());
        if (values.size() <= period) {
            return result;
        }
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;
        for (int index = 1; index <= period; index++) {
            BigDecimal change = values.get(index).subtract(values.get(index - 1));
            gain = gain.add(change.max(BigDecimal.ZERO));
            loss = loss.add(change.min(BigDecimal.ZERO).abs());
        }
        BigDecimal averageGain = gain.divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP);
        BigDecimal averageLoss = loss.divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP);
        result.set(period, rsiValue(averageGain, averageLoss));
        for (int index = period + 1; index < values.size(); index++) {
            BigDecimal change = values.get(index).subtract(values.get(index - 1));
            averageGain = averageGain.multiply(BigDecimal.valueOf(period - 1L))
                    .add(change.max(BigDecimal.ZERO))
                    .divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP);
            averageLoss = averageLoss.multiply(BigDecimal.valueOf(period - 1L))
                    .add(change.min(BigDecimal.ZERO).abs())
                    .divide(BigDecimal.valueOf(period), 18, RoundingMode.HALF_UP);
            result.set(index, rsiValue(averageGain, averageLoss));
        }
        return result;
    }

    private BigDecimal rsiValue(BigDecimal averageGain, BigDecimal averageLoss) {
        if (averageLoss.signum() == 0) {
            return BigDecimal.valueOf(100);
        }
        BigDecimal relativeStrength = averageGain.divide(averageLoss, 18, RoundingMode.HALF_UP);
        return BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(relativeStrength), 12, RoundingMode.HALF_UP)
        );
    }

    private List<BigDecimal> subtract(List<BigDecimal> left, List<BigDecimal> right) {
        List<BigDecimal> result = nullList(Math.min(left.size(), right.size()));
        for (int index = 0; index < result.size(); index++) {
            if (left.get(index) != null && right.get(index) != null) {
                result.set(index, left.get(index).subtract(right.get(index)));
            }
        }
        return result;
    }

    private List<BigDecimal> visible(List<BigDecimal> values, int visibleStart) {
        return new ArrayList<>(values.subList(Math.min(visibleStart, values.size()), values.size()));
    }

    private List<BigDecimal> nullList(int size) {
        return new ArrayList<>(Arrays.asList(new BigDecimal[size]));
    }

    private int warmupCount(ChartQuery query) {
        int warmup = 0;
        if (query.indicators().contains(Indicator.SMA)) warmup = Math.max(warmup, query.smaPeriod());
        if (query.indicators().contains(Indicator.EMA)) warmup = Math.max(warmup, Math.max(50, query.emaPeriod() * 2));
        if (query.indicators().contains(Indicator.RSI)) warmup = Math.max(warmup, query.rsiPeriod() + 1);
        if (query.indicators().contains(Indicator.MACD)) {
            warmup = Math.max(warmup, Math.max(60, query.macdSlowPeriod() + query.macdSignalPeriod()));
        }
        return Math.min(MAX_WARMUP, warmup);
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
        EnumSet<Indicator> indicators = parseIndicators(request.getIndicators());
        int smaPeriod = period(request.getSmaPeriod(), 20, "smaPeriod");
        int emaPeriod = period(request.getEmaPeriod(), 20, "emaPeriod");
        int rsiPeriod = period(request.getRsiPeriod(), 14, "rsiPeriod");
        int macdFast = period(request.getMacdFastPeriod(), 12, "macdFastPeriod");
        int macdSlow = period(request.getMacdSlowPeriod(), 26, "macdSlowPeriod");
        int macdSignal = period(request.getMacdSignalPeriod(), 9, "macdSignalPeriod");
        if (macdFast >= macdSlow) {
            throw new IllegalArgumentException("macdFastPeriod는 macdSlowPeriod보다 작아야 합니다.");
        }
        return new ChartQuery(symbol, interval, limit, request.getEndTime(), indicators,
                smaPeriod, emaPeriod, rsiPeriod, macdFast, macdSlow, macdSignal);
    }

    private EnumSet<Indicator> parseIndicators(String value) {
        EnumSet<Indicator> result = EnumSet.noneOf(Indicator.class);
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String item : value.split(",")) {
            try {
                result.add(Indicator.valueOf(item.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("지원 지표는 SMA, EMA, RSI, MACD입니다.");
            }
        }
        return result;
    }

    private int period(Integer value, int defaultValue, String name) {
        int normalized = value == null ? defaultValue : value;
        if (normalized < 2 || normalized > 200) {
            throw new IllegalArgumentException(name + "는 2부터 200 사이여야 합니다.");
        }
        return normalized;
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

    private enum Indicator { SMA, EMA, RSI, MACD }

    private record ChartQuery(
            String symbol,
            String interval,
            int limit,
            Long endTime,
            EnumSet<Indicator> indicators,
            int smaPeriod,
            int emaPeriod,
            int rsiPeriod,
            int macdFastPeriod,
            int macdSlowPeriod,
            int macdSignalPeriod
    ) {
    }

    private record ChartCacheKey(
            String symbol,
            String interval,
            int limit,
            Long endTime,
            String indicators,
            int smaPeriod,
            int emaPeriod,
            int rsiPeriod,
            int macdFastPeriod,
            int macdSlowPeriod,
            int macdSignalPeriod
    ) {
        private static ChartCacheKey from(ChartQuery query) {
            return new ChartCacheKey(
                    query.symbol(), query.interval(), query.limit(), query.endTime(),
                    query.indicators().toString(), query.smaPeriod(), query.emaPeriod(), query.rsiPeriod(),
                    query.macdFastPeriod(), query.macdSlowPeriod(), query.macdSignalPeriod()
            );
        }
    }

    private record CachedChart(ChartResponseDto response, long expiresAt, long createdAt) {
    }
}
