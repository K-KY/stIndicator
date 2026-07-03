package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import st.indicator.stindicator.domain.utils.candle.Candle;
import st.indicator.stindicator.presentation.dto.ChartIndicatorSignalsDto;
import st.indicator.stindicator.presentation.dto.ChartSignalConfigDto;
import st.indicator.stindicator.presentation.dto.IndicatorSignalEventDto;
import st.indicator.stindicator.presentation.dto.IndicatorSignalSeriesDto;
import st.indicator.stindicator.presentation.dto.IndicatorSignalStateDto;
import st.indicator.stindicator.presentation.dto.SignalDirection;
import st.indicator.stindicator.presentation.dto.SignalEventType;
import st.indicator.stindicator.presentation.dto.SignalReasonCode;
import st.indicator.stindicator.presentation.dto.SignalStrength;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ChartSignalEvaluationService {
    private static final BigDecimal BPS = BigDecimal.valueOf(10_000);
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ADX_EPSILON = new BigDecimal("0.000001");
    private static final BigDecimal MACD_COMPARISON_EPSILON = new BigDecimal("0.00000001");

    public ChartIndicatorSignalsDto evaluate(
            List<Candle> candles,
            List<ChartService.IndicatorPoint> vwapPoints,
            List<ChartService.MacdPoint> macdPoints,
            List<ChartService.IndicatorPoint> rsiPoints,
            List<ChartService.AdxDmiPoint> adxDmiPoints,
            ChartSignalConfigDto requestedConfig,
            long signalConfigVersion,
            long visibleOpenTime
    ) {
        NormalizedConfig config = normalize(requestedConfig);
        Map<Long, Candle> candleByTime = new LinkedHashMap<>();
        candles.forEach(candle -> candleByTime.put(candle.getOpenTime(), candle));
        long now = System.currentTimeMillis();
        return new ChartIndicatorSignalsDto(
                config.vwap() == null ? null : evaluateVwap(candles, candleByTime, vwapPoints, config.vwap(), signalConfigVersion, visibleOpenTime, now),
                config.macd() == null ? null : evaluateMacd(candleByTime, macdPoints, config.macd(), signalConfigVersion, visibleOpenTime, now),
                config.rsi() == null ? null : evaluateRsi(candleByTime, rsiPoints, config.rsi(), signalConfigVersion, visibleOpenTime, now),
                config.adxDmi() == null ? null : evaluateAdxDmi(candleByTime, adxDmiPoints, config.adxDmi(), signalConfigVersion, visibleOpenTime, now)
        );
    }

    public NormalizedConfig normalize(ChartSignalConfigDto config) {
        if (config == null) {
            return new NormalizedConfig(null, null, null, null);
        }
        return new NormalizedConfig(
                normalizeVwap(config.vwap()),
                normalizeMacd(config.macd()),
                normalizeRsi(config.rsi()),
                normalizeAdxDmi(config.adxDmi())
        );
    }

    private VwapConfig normalizeVwap(ChartSignalConfigDto.Vwap value) {
        if (value == null || !Boolean.TRUE.equals(value.enabled())) return null;
        BigDecimal neutral = decimal(value.neutralDistanceBps(), "signalConfig.vwap.neutralDistanceBps", ZERO, BPS);
        BigDecimal strong = decimal(value.strongDistanceBps(), "signalConfig.vwap.strongDistanceBps", new BigDecimal("0.01"), BPS);
        if (neutral.compareTo(strong) >= 0) throw invalid("signalConfig.vwap.neutralDistanceBps", "strongDistanceBps보다 작아야 합니다.");
        return new VwapConfig(policy(value.confirmedCandleOnly(), value.showProvisionalSignal()), neutral,
                positive(value.slopeLookback(), 3, "signalConfig.vwap.slopeLookback"),
                decimal(value.minimumSlopeBps(), "signalConfig.vwap.minimumSlopeBps", ZERO, BPS),
                positive(value.minimumSessionBars(), 10, "signalConfig.vwap.minimumSessionBars"), strong);
    }

    private MacdConfig normalizeMacd(ChartSignalConfigDto.Macd value) {
        if (value == null || !Boolean.TRUE.equals(value.enabled())) return null;
        return new MacdConfig(policy(value.confirmedCandleOnly(), value.showProvisionalSignal()),
                Boolean.TRUE.equals(value.requireZeroLineConfirmation()),
                decimal(value.minimumHistogramBps(), "signalConfig.macd.minimumHistogramBps", ZERO, BPS),
                positive(value.crossConfirmationBars(), 1, "signalConfig.macd.crossConfirmationBars"),
                value.strongRequiresZeroLine() == null || value.strongRequiresZeroLine());
    }

    private RsiConfig normalizeRsi(ChartSignalConfigDto.Rsi value) {
        if (value == null || !Boolean.TRUE.equals(value.enabled())) return null;
        BigDecimal lower = decimal(value.neutralLower(), "signalConfig.rsi.neutralLower", ZERO, HUNDRED);
        BigDecimal upper = decimal(value.neutralUpper(), "signalConfig.rsi.neutralUpper", ZERO, HUNDRED);
        BigDecimal strongLong = decimal(value.strongLongLevel(), "signalConfig.rsi.strongLongLevel", ZERO, HUNDRED);
        BigDecimal strongShort = decimal(value.strongShortLevel(), "signalConfig.rsi.strongShortLevel", ZERO, HUNDRED);
        BigDecimal oversold = decimal(value.oversoldLevel(), "signalConfig.rsi.oversoldLevel", ZERO, HUNDRED);
        BigDecimal overbought = decimal(value.overboughtLevel(), "signalConfig.rsi.overboughtLevel", ZERO, HUNDRED);
        BigDecimal middle = decimal(value.middleLevel(), "signalConfig.rsi.middleLevel", ZERO, HUNDRED);
        if (lower.compareTo(upper) >= 0) throw invalid("signalConfig.rsi.neutralLower", "neutralUpper보다 작아야 합니다.");
        if (strongShort.compareTo(lower) > 0) throw invalid("signalConfig.rsi.strongShortLevel", "neutralLower 이하여야 합니다.");
        if (strongLong.compareTo(upper) < 0) throw invalid("signalConfig.rsi.strongLongLevel", "neutralUpper 이상이어야 합니다.");
        if (oversold.compareTo(overbought) >= 0) throw invalid("signalConfig.rsi.oversoldLevel", "overboughtLevel보다 작아야 합니다.");
        return new RsiConfig(policy(value.confirmedCandleOnly(), value.showProvisionalSignal()), lower, upper, strongLong, strongShort,
                positive(value.slopeLookback(), 2, "signalConfig.rsi.slopeLookback"),
                decimal(value.minimumSlope(), "signalConfig.rsi.minimumSlope", ZERO, HUNDRED), oversold, overbought, middle);
    }

    private AdxDmiConfig normalizeAdxDmi(ChartSignalConfigDto.AdxDmi value) {
        if (value == null || !Boolean.TRUE.equals(value.enabled())) return null;
        BigDecimal weak = decimal(value.weakTrendThreshold(), "signalConfig.adxDmi.weakTrendThreshold", ZERO, HUNDRED);
        BigDecimal strong = decimal(value.strongTrendThreshold(), "signalConfig.adxDmi.strongTrendThreshold", ZERO, HUNDRED);
        if (weak.compareTo(strong) >= 0) throw invalid("signalConfig.adxDmi.weakTrendThreshold", "strongTrendThreshold보다 작아야 합니다.");
        return new AdxDmiConfig(policy(value.confirmedCandleOnly(), value.showProvisionalSignal()), weak, strong,
                decimal(value.minimumDiSpread(), "signalConfig.adxDmi.minimumDiSpread", ZERO, HUNDRED));
    }

    private IndicatorSignalSeriesDto evaluateVwap(List<Candle> candles, Map<Long, Candle> candleByTime,
                                                   List<ChartService.IndicatorPoint> points, VwapConfig config,
                                                   long version, long visibleOpenTime, long now) {
        List<IndicatorSignalStateDto> states = new ArrayList<>();
        List<IndicatorSignalEventDto> events = new ArrayList<>();
        Map<Long, BigDecimal> values = indicatorValues(points);
        Map<String, Integer> sessionBars = new LinkedHashMap<>();
        for (int index = 0; index < points.size(); index++) {
            ChartService.IndicatorPoint point = points.get(index);
            Candle candle = candleByTime.get(point.time());
            if (candle == null || point.value() == null || point.value().signum() <= 0 || !include(candle, config.policy(), now)) continue;
            String session = session(candle.getOpenTime());
            int bars = sessionBars.merge(session, 1, Integer::sum);
            if (index < config.slopeLookback()) continue;
            BigDecimal lookback = points.get(index - config.slopeLookback()).value();
            if (lookback == null || lookback.signum() <= 0) continue;
            BigDecimal signedDistance = bps(candle.getClose().subtract(point.value()), point.value());
            BigDecimal slope = bps(point.value().subtract(lookback), lookback);
            LinkedHashSet<SignalReasonCode> reasons = new LinkedHashSet<>();
            SignalDirection direction = SignalDirection.NEUTRAL;
            SignalStrength strength = SignalStrength.NONE;
            if (bars < config.minimumSessionBars()) {
                reasons.add(SignalReasonCode.INSUFFICIENT_SESSION_BARS);
            } else if (signedDistance.abs().compareTo(config.neutralDistanceBps()) < 0) {
                reasons.add(SignalReasonCode.PRICE_NEAR_VWAP);
            } else {
                boolean slopeUp = slope.compareTo(config.minimumSlopeBps()) >= 0;
                boolean slopeDown = slope.compareTo(config.minimumSlopeBps().negate()) <= 0;
                boolean slopeFlat = !slopeUp && !slopeDown;
                if (signedDistance.signum() > 0) reasons.add(SignalReasonCode.PRICE_ABOVE_VWAP);
                else reasons.add(SignalReasonCode.PRICE_BELOW_VWAP);
                reasons.add(slopeUp ? SignalReasonCode.VWAP_SLOPE_UP : slopeDown ? SignalReasonCode.VWAP_SLOPE_DOWN : SignalReasonCode.VWAP_SLOPE_FLAT);
                if (signedDistance.signum() > 0 && !slopeDown) {
                    direction = SignalDirection.LONG;
                    strength = signedDistance.abs().compareTo(config.strongDistanceBps()) >= 0 && slopeUp ? SignalStrength.STRONG : SignalStrength.WEAK;
                } else if (signedDistance.signum() < 0 && !slopeUp) {
                    direction = SignalDirection.SHORT;
                    strength = signedDistance.abs().compareTo(config.strongDistanceBps()) >= 0 && slopeDown ? SignalStrength.STRONG : SignalStrength.WEAK;
                }
            }
            addState(states, point.time(), direction, strength, confirmed(candle, now), reasons, visibleOpenTime);
            if (index == 0) continue;
            ChartService.IndicatorPoint previousPoint = points.get(index - 1);
            Candle previous = candleByTime.get(previousPoint.time());
            BigDecimal previousVwap = values.get(previousPoint.time());
            if (previous == null || previousVwap == null) continue;
            if (previous.getClose().compareTo(previousVwap) <= 0 && signedDistance.compareTo(config.neutralDistanceBps()) >= 0 && slope.compareTo(config.minimumSlopeBps().negate()) > 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.BULLISH_RECLAIM, SignalDirection.LONG, confirmed(candle, now),
                        List.of(SignalReasonCode.PRICE_ABOVE_VWAP, SignalReasonCode.BULLISH_RECLAIM), visibleOpenTime);
            } else if (previous.getClose().compareTo(previousVwap) >= 0 && signedDistance.compareTo(config.neutralDistanceBps().negate()) <= 0 && slope.compareTo(config.minimumSlopeBps()) < 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.BEARISH_REJECTION, SignalDirection.SHORT, confirmed(candle, now),
                        List.of(SignalReasonCode.PRICE_BELOW_VWAP, SignalReasonCode.BEARISH_REJECTION), visibleOpenTime);
            }
        }
        return new IndicatorSignalSeriesDto(version, states, events);
    }

    private IndicatorSignalSeriesDto evaluateMacd(Map<Long, Candle> candleByTime, List<ChartService.MacdPoint> points,
                                                   MacdConfig config, long version, long visibleOpenTime, long now) {
        List<IndicatorSignalStateDto> states = new ArrayList<>();
        List<IndicatorSignalEventDto> events = new ArrayList<>();
        CrossCandidate candidate = null;
        for (int index = 0; index < points.size(); index++) {
            ChartService.MacdPoint point = points.get(index);
            Candle candle = candleByTime.get(point.time());
            if (candle == null || point.signal() == null || point.histogram() == null || candle.getClose().signum() <= 0 || !include(candle, config.policy(), now)) continue;
            LinkedHashSet<SignalReasonCode> reasons = new LinkedHashSet<>();
            SignalDirection direction = SignalDirection.NEUTRAL;
            SignalStrength strength = SignalStrength.NONE;
            BigDecimal difference = point.macd().subtract(point.signal());
            int relation = relation(difference);
            if (!histogramMatches(difference, point.histogram())) {
                reasons.add(SignalReasonCode.MACD_HISTOGRAM_MISMATCH);
                addState(states, point.time(), direction, strength, confirmed(candle, now), reasons, visibleOpenTime);
                candidate = null;
                continue;
            }
            BigDecimal histogramBps = bps(point.histogram().abs(), candle.getClose());
            if (histogramBps.compareTo(config.minimumHistogramBps()) < 0 || relation == 0) {
                reasons.add(SignalReasonCode.HISTOGRAM_TOO_SMALL);
            } else {
                boolean longDirection = relation > 0;
                reasons.add(longDirection ? SignalReasonCode.MACD_ABOVE_SIGNAL : SignalReasonCode.MACD_BELOW_SIGNAL);
                reasons.add(point.macd().signum() >= 0 ? SignalReasonCode.MACD_ABOVE_ZERO : SignalReasonCode.MACD_BELOW_ZERO);
                reasons.add(point.histogram().signum() >= 0 ? SignalReasonCode.HISTOGRAM_POSITIVE : SignalReasonCode.HISTOGRAM_NEGATIVE);
                boolean zeroAllowed = zeroLineAllowed(config, longDirection ? SignalDirection.LONG : SignalDirection.SHORT, point.macd());
                if (zeroAllowed) {
                    direction = longDirection ? SignalDirection.LONG : SignalDirection.SHORT;
                    ChartService.MacdPoint previous = index > 0 ? points.get(index - 1) : null;
                    boolean expanding = previous != null && previous.histogram() != null
                            && histogramMatches(previous.macd().subtract(previous.signal()), previous.histogram())
                            && point.histogram().abs().compareTo(previous.histogram().abs()) > 0;
                    boolean strongZero = !config.strongRequiresZeroLine() || (longDirection ? point.macd().signum() > 0 : point.macd().signum() < 0);
                    strength = expanding && strongZero ? SignalStrength.STRONG : SignalStrength.WEAK;
                }
            }
            addState(states, point.time(), direction, strength, confirmed(candle, now), reasons, visibleOpenTime);

            if (index > 0) {
                ChartService.MacdPoint previous = points.get(index - 1);
                if (previous.signal() != null) {
                    boolean candleConfirmed = confirmed(candle, now);
                    int previousRelation = relation(previous.macd().subtract(previous.signal()));
                    boolean previousValid = previous.histogram() != null
                            && histogramMatches(previous.macd().subtract(previous.signal()), previous.histogram());
                    boolean golden = previousValid && previousRelation <= 0 && relation > 0;
                    boolean dead = previousValid && previousRelation >= 0 && relation < 0;
                    boolean eligible = histogramBps.compareTo(config.minimumHistogramBps()) >= 0
                            && (golden ? zeroLineAllowed(config, SignalDirection.LONG, point.macd())
                            : dead && zeroLineAllowed(config, SignalDirection.SHORT, point.macd()));
                    if ((golden || dead) && eligible) {
                        candidate = new CrossCandidate(point.time(), golden ? SignalDirection.LONG : SignalDirection.SHORT,
                                candleConfirmed ? 1 : 0);
                    }
                }
            }
            if (candidate != null) {
                boolean maintained = candidate.direction() == SignalDirection.LONG ? relation > 0 : relation < 0;
                boolean zeroStillAllowed = zeroLineAllowed(config, candidate.direction(), point.macd());
                if (!maintained || histogramBps.compareTo(config.minimumHistogramBps()) < 0 || !zeroStillAllowed) {
                    candidate = null;
                } else {
                    boolean candleConfirmed = confirmed(candle, now);
                    if (candidate.originTime() != point.time() && candleConfirmed) {
                        candidate = candidate.incrementConfirmedBars();
                    }
                    if (candidate.confirmedBars() >= config.crossConfirmationBars()) {
                        SignalEventType type = candidate.direction() == SignalDirection.LONG ? SignalEventType.GOLDEN_CROSS : SignalEventType.DEAD_CROSS;
                        addEvent(events, point.time(), candidate.originTime(), type, candidate.direction(), true,
                                List.of(candidate.direction() == SignalDirection.LONG ? SignalReasonCode.MACD_ABOVE_SIGNAL : SignalReasonCode.MACD_BELOW_SIGNAL,
                                        candidate.direction() == SignalDirection.LONG ? SignalReasonCode.GOLDEN_CROSS : SignalReasonCode.DEAD_CROSS), visibleOpenTime);
                        candidate = null;
                    } else if (!candleConfirmed && config.policy().provisional()) {
                        SignalEventType type = candidate.direction() == SignalDirection.LONG ? SignalEventType.GOLDEN_CROSS : SignalEventType.DEAD_CROSS;
                        addEvent(events, point.time(), candidate.originTime(), type, candidate.direction(), false,
                                List.of(candidate.direction() == SignalDirection.LONG ? SignalReasonCode.MACD_ABOVE_SIGNAL : SignalReasonCode.MACD_BELOW_SIGNAL,
                                        candidate.direction() == SignalDirection.LONG ? SignalReasonCode.GOLDEN_CROSS : SignalReasonCode.DEAD_CROSS), visibleOpenTime);
                    }
                }
            }
        }
        return new IndicatorSignalSeriesDto(version, states, events);
    }

    private int relation(BigDecimal difference) {
        if (difference.abs().compareTo(MACD_COMPARISON_EPSILON) <= 0) {
            return 0;
        }
        return difference.signum();
    }

    private boolean histogramMatches(BigDecimal difference, BigDecimal histogram) {
        return histogram != null && difference.subtract(histogram).abs().compareTo(MACD_COMPARISON_EPSILON) <= 0;
    }

    private boolean zeroLineAllowed(MacdConfig config, SignalDirection direction, BigDecimal macd) {
        return !config.requireZeroLineConfirmation()
                || (direction == SignalDirection.LONG ? macd.signum() > 0 : macd.signum() < 0);
    }

    private IndicatorSignalSeriesDto evaluateRsi(Map<Long, Candle> candleByTime, List<ChartService.IndicatorPoint> points,
                                                  RsiConfig config, long version, long visibleOpenTime, long now) {
        List<IndicatorSignalStateDto> states = new ArrayList<>();
        List<IndicatorSignalEventDto> events = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            ChartService.IndicatorPoint point = points.get(index);
            Candle candle = candleByTime.get(point.time());
            if (candle == null || point.value() == null || point.value().compareTo(ZERO) < 0 || point.value().compareTo(HUNDRED) > 0 || !include(candle, config.policy(), now)) continue;
            if (index < config.slopeLookback()) continue;
            BigDecimal slope = point.value().subtract(points.get(index - config.slopeLookback()).value());
            LinkedHashSet<SignalReasonCode> reasons = new LinkedHashSet<>();
            if (point.value().compareTo(config.overboughtLevel()) >= 0) reasons.add(SignalReasonCode.RSI_OVERBOUGHT);
            if (point.value().compareTo(config.oversoldLevel()) <= 0) reasons.add(SignalReasonCode.RSI_OVERSOLD);
            SignalDirection direction = SignalDirection.NEUTRAL;
            SignalStrength strength = SignalStrength.NONE;
            if (point.value().compareTo(config.neutralLower()) >= 0 && point.value().compareTo(config.neutralUpper()) <= 0) {
                reasons.add(SignalReasonCode.RSI_IN_NEUTRAL_ZONE);
            } else if (slope.abs().compareTo(config.minimumSlope()) < 0) {
                reasons.add(SignalReasonCode.RSI_SLOPE_FLAT);
            } else if (point.value().compareTo(config.neutralUpper()) > 0 && slope.compareTo(config.minimumSlope()) >= 0) {
                direction = SignalDirection.LONG;
                strength = point.value().compareTo(config.strongLongLevel()) >= 0 ? SignalStrength.STRONG : SignalStrength.WEAK;
                reasons.add(SignalReasonCode.RSI_ABOVE_MIDDLE);
                reasons.add(SignalReasonCode.RSI_SLOPE_UP);
            } else if (point.value().compareTo(config.neutralLower()) < 0 && slope.compareTo(config.minimumSlope().negate()) <= 0) {
                direction = SignalDirection.SHORT;
                strength = point.value().compareTo(config.strongShortLevel()) <= 0 ? SignalStrength.STRONG : SignalStrength.WEAK;
                reasons.add(SignalReasonCode.RSI_BELOW_MIDDLE);
                reasons.add(SignalReasonCode.RSI_SLOPE_DOWN);
            } else {
                reasons.add(slope.signum() > 0 ? SignalReasonCode.RSI_SLOPE_UP : SignalReasonCode.RSI_SLOPE_DOWN);
            }
            addState(states, point.time(), direction, strength, confirmed(candle, now), reasons, visibleOpenTime);
            if (index == 0) continue;
            BigDecimal previous = points.get(index - 1).value();
            BigDecimal current = point.value();
            if (previous.compareTo(config.oversoldLevel()) <= 0 && current.compareTo(config.oversoldLevel()) > 0 && slope.compareTo(config.minimumSlope()) >= 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.OVERSOLD_RECOVERY, SignalDirection.LONG, confirmed(candle, now),
                        List.of(SignalReasonCode.RSI_OVERSOLD, SignalReasonCode.OVERSOLD_RECOVERY), visibleOpenTime);
            }
            if (previous.compareTo(config.overboughtLevel()) >= 0 && current.compareTo(config.overboughtLevel()) < 0 && slope.compareTo(config.minimumSlope().negate()) <= 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.OVERBOUGHT_REJECTION, SignalDirection.SHORT, confirmed(candle, now),
                        List.of(SignalReasonCode.RSI_OVERBOUGHT, SignalReasonCode.OVERBOUGHT_REJECTION), visibleOpenTime);
            }
            if (previous.compareTo(config.middleLevel()) <= 0 && current.compareTo(config.middleLevel()) > 0 && slope.compareTo(config.minimumSlope()) >= 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.BULLISH_MIDDLE_CROSS, SignalDirection.LONG, confirmed(candle, now),
                        List.of(SignalReasonCode.RSI_ABOVE_MIDDLE, SignalReasonCode.RSI_SLOPE_UP), visibleOpenTime);
            }
            if (previous.compareTo(config.middleLevel()) >= 0 && current.compareTo(config.middleLevel()) < 0 && slope.compareTo(config.minimumSlope().negate()) <= 0) {
                addEvent(events, point.time(), point.time(), SignalEventType.BEARISH_MIDDLE_CROSS, SignalDirection.SHORT, confirmed(candle, now),
                        List.of(SignalReasonCode.RSI_BELOW_MIDDLE, SignalReasonCode.RSI_SLOPE_DOWN), visibleOpenTime);
            }
        }
        return new IndicatorSignalSeriesDto(version, states, events);
    }

    private IndicatorSignalSeriesDto evaluateAdxDmi(Map<Long, Candle> candleByTime, List<ChartService.AdxDmiPoint> points,
                                                     AdxDmiConfig config, long version, long visibleOpenTime, long now) {
        List<IndicatorSignalStateDto> states = new ArrayList<>();
        for (int index = 0; index < points.size(); index++) {
            ChartService.AdxDmiPoint point = points.get(index);
            Candle candle = candleByTime.get(point.time());
            if (candle == null || point.adx() == null || !include(candle, config.policy(), now)) continue;
            BigDecimal spread = point.plusDi().subtract(point.minusDi()).abs();
            LinkedHashSet<SignalReasonCode> reasons = new LinkedHashSet<>();
            if (index > 0 && points.get(index - 1).adx() != null) {
                BigDecimal delta = point.adx().subtract(points.get(index - 1).adx());
                reasons.add(delta.compareTo(ADX_EPSILON) > 0 ? SignalReasonCode.ADX_RISING
                        : delta.compareTo(ADX_EPSILON.negate()) < 0 ? SignalReasonCode.ADX_FALLING : SignalReasonCode.ADX_FLAT);
            }
            SignalDirection direction = SignalDirection.NEUTRAL;
            SignalStrength strength = SignalStrength.NONE;
            if (spread.compareTo(config.minimumDiSpread()) < 0) {
                reasons.add(SignalReasonCode.DI_SPREAD_TOO_SMALL);
            } else if (point.adx().compareTo(config.weakTrendThreshold()) < 0) {
                reasons.add(SignalReasonCode.ADX_BELOW_WEAK_THRESHOLD);
            } else {
                direction = point.plusDi().compareTo(point.minusDi()) > 0 ? SignalDirection.LONG : SignalDirection.SHORT;
                reasons.add(direction == SignalDirection.LONG ? SignalReasonCode.PLUS_DI_DOMINANT : SignalReasonCode.MINUS_DI_DOMINANT);
                if (point.adx().compareTo(config.strongTrendThreshold()) >= 0) {
                    strength = SignalStrength.STRONG;
                    reasons.add(SignalReasonCode.ADX_STRONG);
                    reasons.add(direction == SignalDirection.LONG ? SignalReasonCode.STRONG_BULLISH_TREND : SignalReasonCode.STRONG_BEARISH_TREND);
                } else {
                    strength = SignalStrength.WEAK;
                    reasons.add(SignalReasonCode.ADX_WEAK);
                }
            }
            addState(states, point.time(), direction, strength, confirmed(candle, now), reasons, visibleOpenTime);
        }
        return new IndicatorSignalSeriesDto(version, states, List.of());
    }

    private void addState(List<IndicatorSignalStateDto> states, long time, SignalDirection direction, SignalStrength strength,
                          boolean confirmed, LinkedHashSet<SignalReasonCode> reasons, long visibleOpenTime) {
        if (time < visibleOpenTime) return;
        states.add(new IndicatorSignalStateDto(time, direction, direction == SignalDirection.NEUTRAL ? SignalStrength.NONE : strength,
                confirmed, List.copyOf(reasons)));
    }

    private void addEvent(List<IndicatorSignalEventDto> events, long time, long originTime, SignalEventType type,
                          SignalDirection direction, boolean confirmed, List<SignalReasonCode> reasons, long visibleOpenTime) {
        if (time < visibleOpenTime) return;
        boolean duplicate = events.stream().anyMatch(event -> event.type() == type && event.originTime() == originTime);
        if (!duplicate) events.add(new IndicatorSignalEventDto(time, originTime, type, direction, confirmed, new ArrayList<>(new LinkedHashSet<>(reasons))));
    }

    private Map<Long, BigDecimal> indicatorValues(List<ChartService.IndicatorPoint> points) {
        Map<Long, BigDecimal> values = new LinkedHashMap<>();
        points.forEach(point -> values.put(point.time(), point.value()));
        return values;
    }

    private boolean include(Candle candle, CandlePolicy policy, long now) {
        return confirmed(candle, now) || (!policy.confirmedOnly() && policy.provisional());
    }

    private boolean confirmed(Candle candle, long now) {
        return candle.getCloseTime() != null && candle.getCloseTime() <= now;
    }

    private String session(long time) {
        return Instant.ofEpochMilli(time).atZone(ZoneOffset.UTC).toLocalDate().toString();
    }

    private BigDecimal bps(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.signum() <= 0) throw new IllegalArgumentException("시그널 bps 계산 기준값은 0보다 커야 합니다.");
        return numerator.multiply(BPS).divide(denominator, 18, RoundingMode.HALF_UP);
    }

    private CandlePolicy policy(Boolean confirmedOnly, Boolean provisional) {
        return new CandlePolicy(confirmedOnly == null || confirmedOnly, Boolean.TRUE.equals(provisional));
    }

    private int positive(Integer value, int fallback, String field) {
        int normalized = value == null ? fallback : value;
        if (normalized < 1 || normalized > 1000) throw invalid(field, "1 이상 1000 이하의 정수여야 합니다.");
        return normalized;
    }

    private BigDecimal decimal(BigDecimal value, String field, BigDecimal min, BigDecimal max) {
        BigDecimal normalized = value == null ? defaultValue(field) : value;
        if (normalized.compareTo(min) < 0 || normalized.compareTo(max) > 0) throw invalid(field, min + " 이상 " + max + " 이하여야 합니다.");
        return normalized;
    }

    private BigDecimal defaultValue(String field) {
        if (field.contains("neutralDistance")) return BigDecimal.valueOf(5);
        if (field.contains("strongDistance")) return BigDecimal.valueOf(20);
        if (field.contains("minimumSlopeBps") || field.contains("minimumHistogram")) return BigDecimal.ONE;
        if (field.contains("neutralLower")) return BigDecimal.valueOf(45);
        if (field.contains("neutralUpper")) return BigDecimal.valueOf(55);
        if (field.contains("strongLong")) return BigDecimal.valueOf(60);
        if (field.contains("strongShort")) return BigDecimal.valueOf(40);
        if (field.contains("minimumSlope")) return new BigDecimal("0.5");
        if (field.contains("oversoldLevel")) return BigDecimal.valueOf(30);
        if (field.contains("overboughtLevel")) return BigDecimal.valueOf(70);
        if (field.contains("middleLevel")) return BigDecimal.valueOf(50);
        if (field.contains("weakTrend")) return BigDecimal.valueOf(20);
        if (field.contains("strongTrend")) return BigDecimal.valueOf(25);
        if (field.contains("minimumDiSpread")) return BigDecimal.valueOf(2);
        return ZERO;
    }

    private IllegalArgumentException invalid(String field, String message) {
        return new IllegalArgumentException(field + "은(는) " + message);
    }

    public record NormalizedConfig(VwapConfig vwap, MacdConfig macd, RsiConfig rsi, AdxDmiConfig adxDmi) {
        public boolean enabled() { return vwap != null || macd != null || rsi != null || adxDmi != null; }
        public int extraWarmup() {
            int result = 0;
            if (vwap != null) result = Math.max(result, vwap.slopeLookback() + 1);
            if (macd != null) result = Math.max(result, macd.crossConfirmationBars() + 1);
            if (rsi != null) result = Math.max(result, rsi.slopeLookback() + 1);
            if (adxDmi != null) result = Math.max(result, 1);
            return result;
        }
    }
    public record CandlePolicy(boolean confirmedOnly, boolean provisional) { }
    public record VwapConfig(CandlePolicy policy, BigDecimal neutralDistanceBps, int slopeLookback, BigDecimal minimumSlopeBps, int minimumSessionBars, BigDecimal strongDistanceBps) { }
    public record MacdConfig(CandlePolicy policy, boolean requireZeroLineConfirmation, BigDecimal minimumHistogramBps, int crossConfirmationBars, boolean strongRequiresZeroLine) { }
    public record RsiConfig(CandlePolicy policy, BigDecimal neutralLower, BigDecimal neutralUpper, BigDecimal strongLongLevel,
                            BigDecimal strongShortLevel, int slopeLookback, BigDecimal minimumSlope,
                            BigDecimal oversoldLevel, BigDecimal overboughtLevel, BigDecimal middleLevel) { }
    public record AdxDmiConfig(CandlePolicy policy, BigDecimal weakTrendThreshold, BigDecimal strongTrendThreshold, BigDecimal minimumDiSpread) { }
    private record CrossCandidate(long originTime, SignalDirection direction, int confirmedBars) {
        private CrossCandidate incrementConfirmedBars() {
            return new CrossCandidate(originTime, direction, confirmedBars + 1);
        }
    }
}
