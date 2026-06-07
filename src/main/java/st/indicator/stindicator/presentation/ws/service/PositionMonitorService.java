package st.indicator.stindicator.presentation.ws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.domain.entity.MonitorOrderType;
import st.indicator.stindicator.domain.entity.PositionDirection;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.domain.entity.PositionMonitorStatus;
import st.indicator.stindicator.domain.repository.PositionMonitorRepository;
import st.indicator.stindicator.infra.ws.MultiPlexManager;
import st.indicator.stindicator.presentation.ws.dto.MonitorEventType;
import st.indicator.stindicator.presentation.ws.dto.MonitorSocketEventDto;
import st.indicator.stindicator.presentation.ws.dto.MonitorStartRequestDto;
import st.indicator.stindicator.presentation.ws.dto.PositionMonitorResponseDto;
import st.indicator.stindicator.presentation.ws.publisher.MonitorEventPublisher;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PositionMonitorService {
    private static final Logger log = LoggerFactory.getLogger(PositionMonitorService.class);
    private static final int SCALE = 8;

    private final PositionMonitorRepository positionMonitorRepository;
    private final MultiPlexManager multiPlexManager;
    private final MonitorService monitorService;
    private final MonitorEventPublisher monitorEventPublisher;
    private final Map<Long, PositionMonitorRuntimeState> runtimeStates = new ConcurrentHashMap<>();

    public PositionMonitorService(PositionMonitorRepository positionMonitorRepository,
                                  MultiPlexManager multiPlexManager,
                                  MonitorService monitorService,
                                  MonitorEventPublisher monitorEventPublisher) {
        this.positionMonitorRepository = positionMonitorRepository;
        this.multiPlexManager = multiPlexManager;
        this.monitorService = monitorService;
        this.monitorEventPublisher = monitorEventPublisher;
    }

    public PositionMonitor start(MonitorStartRequestDto request) {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitor positionMonitor = new PositionMonitor(
                null,
                request.getUserId(),
                request.getSymbol(),
                request.getDirection(),
                request.getEntryPrice(),
                request.getEntryPrice(),
                request.getQuantity(),
                request.getLeverage(),
                BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP),
                request.getTrailingGapPercent().negate().setScale(SCALE, RoundingMode.HALF_UP),
                request.getTrailingGapPercent().negate().setScale(SCALE, RoundingMode.HALF_UP),
                request.getTrailingGapPercent().setScale(SCALE, RoundingMode.HALF_UP),
                request.getCloseOrderType() == null ? MonitorOrderType.MARKET : request.getCloseOrderType(),
                request.getCloseLimitPrice(),
                PositionMonitorStatus.ACTIVE,
                now,
                now
        );
        PositionMonitor saved = positionMonitorRepository.save(positionMonitor);
        runtimeStates.put(saved.getId(), new PositionMonitorRuntimeState(saved));
        ensureSymbolSubscribed(saved.getSymbol());
        publishToUser(saved.getUserId(), saved, MonitorEventType.POSITION_UPDATE);
        return saved;
    }

    public Optional<PositionMonitor> stop(Long userId, Long monitorId) {
        PositionMonitorRuntimeState runtimeState = runtimeStates.get(monitorId);
        PositionMonitor current = runtimeState != null ? runtimeState.snapshot().get() : positionMonitorRepository.findById(monitorId).orElse(null);
        if (current == null || !current.getUserId().equals(userId)) {
            return Optional.empty();
        }

        PositionMonitor stopped = copyOf(current, current.getCurrentPrice(), current.getCurrentProfitPercent(),
                current.getHighestProfitPercent(), current.getTrailingStopPercent(),
                current.getCurrentStopLine(), PositionMonitorStatus.STOPPED);
        PositionMonitor saved = positionMonitorRepository.save(stopped);
        runtimeStates.remove(monitorId);
        cleanupSubscriptionIfUnused(saved.getSymbol());
        publishToUser(saved.getUserId(), saved, MonitorEventType.POSITION_UPDATE);
        return Optional.of(saved);
    }

    public List<PositionMonitor> list(Long userId) {
        return positionMonitorRepository.findAllByUserId(userId);
    }

    public void restoreActiveMonitors() {
        List<PositionMonitor> activeMonitors = positionMonitorRepository.findAllByStatus(PositionMonitorStatus.ACTIVE);
        for (PositionMonitor activeMonitor : activeMonitors) {
            runtimeStates.put(activeMonitor.getId(), new PositionMonitorRuntimeState(activeMonitor));
            ensureSymbolSubscribed(activeMonitor.getSymbol());
        }
        log.info("restored active monitors size={}", activeMonitors.size());
    }

    public void handlePriceTick(String symbol, BigDecimal currentPrice, long eventTime) {
        runtimeStates.values().stream()
                .filter(runtime -> runtime.snapshot().get().getStatus() == PositionMonitorStatus.ACTIVE)
                .filter(runtime -> runtime.snapshot().get().getSymbol().equalsIgnoreCase(symbol))
                .forEach(runtime -> processPriceUpdate(runtime, currentPrice, eventTime));
    }

    public void markClosed(PositionMonitor positionMonitor) {
        runtimeStates.remove(positionMonitor.getId());
        cleanupSubscriptionIfUnused(positionMonitor.getSymbol());
    }

    private void processPriceUpdate(PositionMonitorRuntimeState runtime, BigDecimal currentPrice, long eventTime) {
        if (eventTime <= runtime.lastEventTime().get()) {
            return;
        }
        runtime.lastEventTime().set(eventTime);

        while (true) {
            PositionMonitor before = runtime.snapshot().get();
            if (before.getStatus() != PositionMonitorStatus.ACTIVE) {
                return;
            }

            BigDecimal profitPercent = calculateProfitPercent(before, currentPrice);
            BigDecimal highestProfit = before.getHighestProfitPercent().max(profitPercent);
            BigDecimal stopLineCandidate = highestProfit.subtract(before.getTrailingGapPercent()).setScale(SCALE, RoundingMode.HALF_UP);
            BigDecimal newStopLine = before.getCurrentStopLine().max(stopLineCandidate);

            PositionMonitor after = copyOf(
                    before,
                    currentPrice,
                    profitPercent,
                    highestProfit,
                    before.getTrailingStopPercent().max(newStopLine),
                    newStopLine,
                    before.getStatus()
            );

            if (runtime.snapshot().compareAndSet(before, after)) {
                positionMonitorRepository.save(after);
                publishPriceUpdate(after, currentPrice);
                if (newStopLine.compareTo(before.getCurrentStopLine()) > 0) {
                    publishToUser(after.getUserId(), after, MonitorEventType.TRAILING_STOP_UPDATED);
                }
                if (profitPercent.compareTo(newStopLine) <= 0 && runtime.closeTriggered().compareAndSet(false, true)) {
                    publishToUser(after.getUserId(), after, MonitorEventType.STOP_TRIGGERED);
                    monitorEventPublisher.publishStopTriggered(after);
                }
                return;
            }
        }
    }

    public PositionMonitor updateAfterClose(PositionMonitor positionMonitor, PositionMonitorStatus status) {
        PositionMonitor updated = copyOf(positionMonitor, positionMonitor.getCurrentPrice(),
                positionMonitor.getCurrentProfitPercent(), positionMonitor.getHighestProfitPercent(),
                positionMonitor.getTrailingStopPercent(), positionMonitor.getCurrentStopLine(), status);
        PositionMonitor saved = positionMonitorRepository.save(updated);
        publishToUser(saved.getUserId(), saved, MonitorEventType.POSITION_UPDATE);
        return saved;
    }

    private void publishPriceUpdate(PositionMonitor positionMonitor, BigDecimal currentPrice) {
        monitorService.publishMonitorEvent(positionMonitor.getSymbol(), new MonitorSocketEventDto(
                MonitorEventType.PRICE_UPDATE,
                positionMonitor.getUserId(),
                positionMonitor.getSymbol(),
                positionMonitor.getId(),
                Map.of(
                        "currentPrice", currentPrice,
                        "currentProfitPercent", positionMonitor.getCurrentProfitPercent()
                )
        ));
        publishToUser(positionMonitor.getUserId(), positionMonitor, MonitorEventType.POSITION_UPDATE);
    }

    private void publishToUser(Long userId, PositionMonitor positionMonitor, MonitorEventType type) {
        monitorService.publishMonitorEvent(positionMonitor.getSymbol(), new MonitorSocketEventDto(
                type,
                userId,
                positionMonitor.getSymbol(),
                positionMonitor.getId(),
                PositionMonitorResponseDto.from(positionMonitor)
        ));
    }

    private void ensureSymbolSubscribed(String symbol) {
        multiPlexManager.subscribeMarkPrice(symbol);
    }

    private void cleanupSubscriptionIfUnused(String symbol) {
        boolean hasActive = runtimeStates.values().stream()
                .map(state -> state.snapshot().get())
                .anyMatch(positionMonitor -> positionMonitor.getStatus() == PositionMonitorStatus.ACTIVE
                        && positionMonitor.getSymbol().equalsIgnoreCase(symbol));
        if (!hasActive && !monitorService.hasSubscribers(symbol)) {
            multiPlexManager.unsubscribeMarkPrice(symbol);
        }
    }

    private BigDecimal calculateProfitPercent(PositionMonitor positionMonitor, BigDecimal currentPrice) {
        BigDecimal direction = positionMonitor.getDirection() == PositionDirection.BUY ? BigDecimal.ONE : BigDecimal.ONE.negate();
        BigDecimal diff = currentPrice.subtract(positionMonitor.getEntryPrice()).multiply(direction);
        return diff.divide(positionMonitor.getEntryPrice(), SCALE, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .multiply(positionMonitor.getLeverage())
                .setScale(SCALE, RoundingMode.HALF_UP);
    }

    private PositionMonitor copyOf(PositionMonitor source, BigDecimal currentPrice, BigDecimal currentProfitPercent,
                                   BigDecimal highestProfitPercent, BigDecimal trailingStopPercent,
                                   BigDecimal currentStopLine, PositionMonitorStatus status) {
        return new PositionMonitor(
                source.getId(),
                source.getUserId(),
                source.getSymbol(),
                source.getDirection(),
                source.getEntryPrice(),
                currentPrice,
                source.getQuantity(),
                source.getLeverage(),
                currentProfitPercent,
                highestProfitPercent,
                trailingStopPercent,
                currentStopLine,
                source.getTrailingGapPercent(),
                source.getCloseOrderType(),
                source.getCloseLimitPrice(),
                status,
                source.getCreatedAt(),
                LocalDateTime.now()
        );
    }
}
