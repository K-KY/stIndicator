package st.indicator.stindicator.application.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import st.indicator.stindicator.application.dto.AtrOrderCommand;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.domain.entity.*;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionJournalEntity;
import st.indicator.stindicator.infra.connector.entity.ManagedStopHistoryEntity;
import st.indicator.stindicator.infra.connector.entity.PendingOrderEntity;
import st.indicator.stindicator.infra.connector.repository.ManagedPositionJpaRepository;
import st.indicator.stindicator.infra.connector.repository.ManagedPositionJournalJpaRepository;
import st.indicator.stindicator.infra.connector.repository.ManagedStopHistoryJpaRepository;
import st.indicator.stindicator.infra.connector.repository.PendingOrderJpaRepository;
import st.indicator.stindicator.infra.ws.MultiPlexManager;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionResponseDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalRequestDto;
import st.indicator.stindicator.presentation.dto.AddRaisingStopRequestDto;
import st.indicator.stindicator.presentation.dto.UpdateManagedPositionTriggerBasisRequestDto;
import st.indicator.stindicator.presentation.dto.UpdateManagedPositionModeRequestDto;
import st.indicator.stindicator.presentation.dto.UpdatePendingOrderConditionsRequestDto;
import st.indicator.stindicator.presentation.ws.publisher.OrderTradeUpdateEvent;
import st.indicator.stindicator.presentation.ws.publisher.PriceTickEvent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ManagedTradeService {
    private static final Logger log = LoggerFactory.getLogger(ManagedTradeService.class);
    private static final int MAX_UNMATCHED_ORDER_UPDATE_KEYS = 512;
    private final ClientService clientService;
    private final ExchangeConnector exchangeConnector;
    private final PendingOrderJpaRepository pendingOrderRepository;
    private final ManagedPositionJpaRepository managedPositionRepository;
    private final ManagedPositionJournalJpaRepository managedPositionJournalRepository;
    private final ManagedStopHistoryJpaRepository managedStopHistoryRepository;
    private final MultiPlexManager multiPlexManager;
    private final MonitorService monitorService;
    private final Set<Long> closingPositionIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> fillingPendingOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> fillingTestOrderIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> evaluatingPositionIds = ConcurrentHashMap.newKeySet();
    private final Set<String> importingExternalPositionKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> recordedStopUpdates = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService testVolatilityExecutor = Executors.newScheduledThreadPool(2);
    private final Map<Long, ScheduledFuture<?>> testVolatilityTasks = new ConcurrentHashMap<>();
    private final Map<String, ManagedOrderSymbolRule> orderRuleCache = new ConcurrentHashMap<>();
    private final Map<String, OrderTradeUpdateEvent> unmatchedOrderUpdates = new ConcurrentHashMap<>();

    public ManagedTradeService(ClientService clientService,
                               ExchangeConnector exchangeConnector,
                               PendingOrderJpaRepository pendingOrderRepository,
                               ManagedPositionJpaRepository managedPositionRepository,
                               ManagedPositionJournalJpaRepository managedPositionJournalRepository,
                               ManagedStopHistoryJpaRepository managedStopHistoryRepository,
                               MultiPlexManager multiPlexManager,
                               MonitorService monitorService) {
        this.clientService = clientService;
        this.exchangeConnector = exchangeConnector;
        this.pendingOrderRepository = pendingOrderRepository;
        this.managedPositionRepository = managedPositionRepository;
        this.managedPositionJournalRepository = managedPositionJournalRepository;
        this.managedStopHistoryRepository = managedStopHistoryRepository;
        this.multiPlexManager = multiPlexManager;
        this.monitorService = monitorService;
    }

    @PostConstruct
    @Transactional
    public void restoreStreams() {
        List<ManagedPositionEntity> activePositions = managedPositionRepository.findAllByStatusOrderByOpenedAtDesc(ManagedPositionStatus.ACTIVE);
        activePositions.forEach(this::migrateLegacyPositionIfNeeded);
        List<PendingOrderEntity> pendingOrders = pendingOrderRepository.findAllByStatusOrderByCreatedAtDesc(PendingOrderStatus.PENDING);
        pendingOrders.forEach(this::repairPendingSizingIfNeeded);
        pendingOrders.forEach(this::normalizePendingPolicyIfNeeded);
        pendingOrders.forEach(order -> multiPlexManager.subscribeMarkPrice(order.getSymbol()));
        activePositions.forEach(position -> multiPlexManager.subscribeMarkPrice(position.getSymbol()));
        log.info("managed trade restore pending={}, active={}",
                pendingOrders.size(),
                activePositions.size());
    }

    @PreDestroy
    public void shutdownTestVolatilityExecutor() {
        testVolatilityTasks.values().forEach(task -> task.cancel(true));
        testVolatilityExecutor.shutdownNow();
    }

    private void migrateLegacyPositionIfNeeded(ManagedPositionEntity position) {
        if (!position.isLegacyManagedPolicy()) {
            return;
        }
        BigDecimal previousStopPrice = position.getCurrentStopPrice();
        log.warn("legacy managed position detected id={}, symbol={}, mode={}, entryPrice={}, initialStop={}, currentStop={}, target={}",
                position.getId(), position.getSymbol(), position.getMode(), position.getEntryPrice(),
                position.getInitialStopPrice(), position.getCurrentStopPrice(), position.getTargetPrice());
        position.applyLegacyFixedPolicy();
        managedPositionRepository.save(position);
        if (previousStopPrice != null && position.getCurrentStopPrice() != null
                && previousStopPrice.compareTo(position.getCurrentStopPrice()) != 0) {
            recordStopUpdate(
                    position,
                    position.getCurrentPrice() == null ? position.getEntryPrice() : position.getCurrentPrice(),
                    previousStopPrice,
                    position.getCurrentStopPrice(),
                    position.getUnrealizedPnl(),
                    null,
                    ManagedStopUpdateReason.MIGRATION
            );
        }
        log.warn("legacy managed position migrated id={}, symbol={}, mode={}, raiseStopEnabled={}, initialStop={}, currentStop={}, target={}",
                position.getId(), position.getSymbol(), position.getMode(), position.isRaiseStopEnabled(),
                position.getInitialStopPrice(), position.getCurrentStopPrice(), position.getTargetPrice());
    }

    @Transactional
    public PendingOrderEntity createAtrLimitOrder(Long userId, ManagedAtrOrderRequestDto request) {
        validateCreateRequest(request);
        AtrOrderCommand command = new AtrOrderCommand(
                request.getSymbol(),
                request.getSide(),
                valueOrDefault(request.getInterval(), "4h"),
                valueOrDefault(request.getLimit(), "150"),
                request.getAtrPeriod() == null ? 14 : request.getAtrPeriod(),
                request.getRiskPercent() == null ? BigDecimal.ONE : request.getRiskPercent(),
                request.getAtrMultiplier() == null ? BigDecimal.ONE : request.getAtrMultiplier(),
                request.getLeverage() == null ? BigDecimal.ONE : request.getLeverage(),
                "LIMIT",
                valueOrDefault(request.getTimeInForce(), "GTC"),
                request.getEntryPrice(),
                request.getStopTriggerBasis(),
                request.getTakeProfitTriggerBasis()
        );
        AtrOrderPreview preview = clientService.previewAtrOrder(command);
        boolean testOrder = request.getExecutionMode() == TradeExecutionMode.TEST;
        ManagedTradeRiskPolicy.requireSufficientBalance(preview);
        log.info("managed ATR margin validated symbol={}, executionMode={}, availableBalance={}, requiredMargin={}, shortage={}, orderable={}",
                preview.getSymbol(), request.getExecutionMode(), preview.getAvailableBalance(),
                preview.getRequiredMargin(), preview.getShortage(), preview.isOrderable());
        Order order = testOrder
                ? testEntryOrder(preview)
                : placeRealEntryOrder(request, preview);
        PendingOrderEntity pending = PendingOrderEntity.create(
                userId,
                preview.getSymbol(),
                normalizeSide(preview.getSide()),
                order.getOrderId(),
                order.getClientOrderId(),
                preview.getEntryPrice(),
                preview.getQuantity(),
                preview.getStopPrice(),
                preview.getTargetPrice(),
                preview.getPossibleLoss(),
                preview.getPossibleProfit(),
                preview.getLeverage(),
                preview.getRequiredMargin(),
                request.getStopTriggerBasis(),
                request.getTakeProfitTriggerBasis(),
                preview.getAtr(),
                preview.getAtrMultiplier(),
                preview.getRiskPercent(),
                request.getMode() == null ? ManagedOrderMode.FIXED_TP_SL : request.getMode(),
                request.isRaiseStopEnabled(),
                request.getRaiseTriggerType(),
                request.getRaiseTriggerValue(),
                request.getRaiseStopType(),
                request.getRaiseStopValue(),
                request.getExecutionMode()
        );
        normalizePendingPolicyIfNeeded(pending);
        PendingOrderEntity saved = pendingOrderRepository.save(pending);
        multiPlexManager.subscribeMarkPrice(saved.getSymbol());
        if (testOrder) {
            log.info("managed test pending order created id={}, symbol={}, entryPrice={}, quantity={}, leverage={}, requiredMargin={}, BinanceOrderApiCalled=false",
                    saved.getId(), saved.getSymbol(), saved.getEntryPrice(), saved.getQuantity(),
                    saved.getLeverage(), saved.getRequiredMargin());
        } else {
            log.info("managed real pending order created id={}, symbol={}, orderId={}, quantity={}, requiredMargin={}",
                    saved.getId(), saved.getSymbol(), saved.getOrderId(), saved.getQuantity(), saved.getRequiredMargin());
            reconcileRealEntryOrder(saved.getId(), order);
        }
        return saved;
    }

    private void reconcileRealEntryOrder(Long pendingOrderId, Order order) {
        PendingOrderEntity pending = pendingOrderRepository.findById(pendingOrderId).orElse(null);
        if (pending == null || pending.getStatus() != PendingOrderStatus.PENDING || pending.isTestOrder()) {
            return;
        }
        OrderTradeUpdateEvent buffered = removeBufferedOrderUpdate(pending);
        if (buffered != null) {
            log.info("replaying early Binance fill event pendingId={}, orderId={}, clientOrderId={}, status={}",
                    pending.getId(), buffered.orderId(), buffered.clientOrderId(), buffered.orderStatus());
            handlePendingOrderUpdate(pending, buffered);
            return;
        }
        if (order != null && "FILLED".equalsIgnoreCase(order.getStatus())) {
            fillPendingOrder(pending, order.getAvgPrice(), order.getExecutedQty(), "BINANCE_ORDER_RESPONSE");
        }
    }

    private Order placeRealEntryOrder(ManagedAtrOrderRequestDto request, AtrOrderPreview preview) {
        changeLeverage(preview.getSymbol(), preview.getLeverage());
        return clientService.order(new st.indicator.stindicator.application.dto.OrderCommand(
                preview.getSymbol(),
                normalizeSide(preview.getSide()),
                "LIMIT",
                valueOrDefault(request.getTimeInForce(), "GTC"),
                preview.getQuantity().toPlainString(),
                preview.getEntryPrice().toPlainString()
        ));
    }

    private Order testEntryOrder(AtrOrderPreview preview) {
        String id = "TEST-" + UUID.randomUUID();
        return new Order(id, preview.getSymbol(), "FILLED", id,
                preview.getEntryPrice(), preview.getEntryPrice(), preview.getQuantity(), preview.getQuantity(),
                preview.getQuantity(), preview.getEntryPrice().multiply(preview.getQuantity()),
                "GTC", "LIMIT", false, false, normalizeSide(preview.getSide()),
                "BOTH", BigDecimal.ZERO, null, false, "LIMIT", null, null, null,
                String.valueOf(System.currentTimeMillis()));
    }

    @Transactional
    public List<PendingOrderEntity> pendingOrders(Long userId) {
        List<PendingOrderEntity> pendingOrders = pendingOrderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, PendingOrderStatus.PENDING);
        pendingOrders.forEach(this::repairPendingSizingIfNeeded);
        pendingOrders.forEach(this::normalizePendingPolicyIfNeeded);
        pendingOrders.stream()
                .filter(order -> !order.isTestOrder())
                .forEach(this::reconcilePendingRealOrder);
        return pendingOrderRepository.findAllByUserIdAndStatusOrderByCreatedAtDesc(userId, PendingOrderStatus.PENDING);
    }

    private void reconcilePendingRealOrder(PendingOrderEntity pending) {
        if (pending == null
                || pending.isTestOrder()
                || pending.getStatus() != PendingOrderStatus.PENDING
                || isBlank(pending.getOrderId())) {
            return;
        }
        try {
            Order exchangeOrder = clientService.getOrderDetail(pending.getSymbol(), pending.getOrderId());
            if (exchangeOrder == null) {
                return;
            }
            if ("FILLED".equalsIgnoreCase(exchangeOrder.getStatus())) {
                log.warn("recovering filled Binance order missing managed position pendingId={}, symbol={}, orderId={}",
                        pending.getId(), pending.getSymbol(), pending.getOrderId());
                fillPendingOrder(pending, exchangeOrder.getAvgPrice(), exchangeOrder.getExecutedQty(), "BINANCE_ORDER_RECONCILIATION");
                return;
            }
            if ("CANCELED".equalsIgnoreCase(exchangeOrder.getStatus())) {
                pending.mark(PendingOrderStatus.CANCELED);
                pendingOrderRepository.save(pending);
                return;
            }
            if ("EXPIRED".equalsIgnoreCase(exchangeOrder.getStatus())
                    || "EXPIRED_IN_MATCH".equalsIgnoreCase(exchangeOrder.getStatus())) {
                pending.mark(PendingOrderStatus.EXPIRED);
                pendingOrderRepository.save(pending);
            }
        } catch (RuntimeException error) {
            log.warn("managed pending order reconciliation failed pendingId={}, symbol={}, orderId={}",
                    pending.getId(), pending.getSymbol(), pending.getOrderId(), error);
        }
    }

    @Transactional
    public PendingOrderEntity pendingOrder(Long userId, Long id) {
        PendingOrderEntity pending = pendingOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대기 주문을 찾을 수 없습니다: " + id));
        assertOwner(pending.getUserId(), userId);
        pending = repairPendingSizingIfNeeded(pending);
        normalizePendingPolicyIfNeeded(pending);
        return pending;
    }

    @Transactional
    public PendingOrderEntity cancelPendingOrder(Long userId, Long id) {
        PendingOrderEntity pending = pendingOrder(userId, id);
        if (pending.getStatus() != PendingOrderStatus.PENDING) {
            return pending;
        }
        if (!pending.isTestOrder()) {
            clientService.cancelOrder(pending.getSymbol(), pending.getOrderId());
        }
        pending.mark(PendingOrderStatus.CANCELED);
        return pendingOrderRepository.save(pending);
    }

    @Transactional
    public PendingOrderEntity updatePendingConditions(Long userId, Long id, UpdatePendingOrderConditionsRequestDto request) {
        PendingOrderEntity pending = pendingOrder(userId, id);
        if (pending.getStatus() != PendingOrderStatus.PENDING) {
            throw new IllegalArgumentException("대기중인 주문만 조건을 수정할 수 있습니다.");
        }
        pending = repairPendingSizingIfNeeded(pending);
        normalizePendingPolicyIfNeeded(pending);
        boolean raisingStopOnly = pending.getMode() == ManagedOrderMode.RAISING_STOP_ONLY;
        ExitPlan exitPlan = recalculatePendingExitPlan(
                pending,
                request.getStopTriggerBasis(),
                raisingStopOnly ? null : request.getTakeProfitTriggerBasis()
        );
        pending.updateConditions(
                exitPlan.stopPrice() == null ? positiveOrNull(request.getStopPrice(), "stopPrice") : exitPlan.stopPrice(),
                raisingStopOnly ? null : exitPlan.targetPrice() == null ? positiveOrNull(request.getTargetPrice(), "targetPrice") : exitPlan.targetPrice(),
                exitPlan.possibleLoss() == null ? positiveOrNull(request.getPossibleLoss(), "possibleLoss") : exitPlan.possibleLoss(),
                raisingStopOnly ? null : exitPlan.possibleProfit() == null ? positiveOrNull(request.getPossibleProfit(), "possibleProfit") : exitPlan.possibleProfit(),
                exitPlan.stopTriggerBasis(),
                raisingStopOnly ? null : exitPlan.takeProfitTriggerBasis(),
                request.getRaiseTriggerType(),
                positiveOrNull(request.getRaiseTriggerValue(), "raiseTriggerValue"),
                request.getRaiseStopType(),
                positiveOrNull(request.getRaiseStopValue(), "raiseStopValue")
        );
        if (raisingStopOnly) {
            pending.clearTakeProfit();
        }
        return pendingOrderRepository.save(pending);
    }

    private ExitPlan recalculatePendingExitPlan(PendingOrderEntity pending, TriggerBasis requestedStopBasis,
                                                TriggerBasis requestedTakeProfitBasis) {
        TriggerBasis stopBasis = requestedStopBasis == null ? pending.getStopTriggerBasis() : requestedStopBasis;
        TriggerBasis takeProfitBasis = requestedTakeProfitBasis == null ? pending.getTakeProfitTriggerBasis() : requestedTakeProfitBasis;
        if (requestedStopBasis == null && requestedTakeProfitBasis == null) {
            return new ExitPlan(null, null, null, null, null, null);
        }
        BigDecimal stopDistance = pending.getAtr() == null || pending.getAtrMultiplier() == null
                ? pending.getStopPrice().subtract(pending.getEntryPrice()).abs()
                : pending.getAtr().multiply(pending.getAtrMultiplier());
        BigDecimal riskPercent = pending.getRiskPercent() == null ? BigDecimal.ONE : pending.getRiskPercent();
        boolean longSide = "BUY".equalsIgnoreCase(pending.getSide());

        BigDecimal stopPrice = null;
        BigDecimal targetPrice = null;
        BigDecimal possibleLoss = null;
        BigDecimal possibleProfit = null;

        if (requestedStopBasis != null) {
            if (stopBasis == TriggerBasis.PNL_PERCENT) {
                if (!canCalculateMarginPnl(pending)) {
                    log.warn("skip pending stop pnl recalculation id={}, symbol={}, quantity={}, requiredMargin={}, requestedBasis={}",
                            pending.getId(), pending.getSymbol(), pending.getQuantity(), pending.getRequiredMargin(), stopBasis);
                    stopBasis = null;
                } else {
                    possibleLoss = pending.getRequiredMargin().multiply(riskPercent)
                            .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
                    BigDecimal priceMove = possibleLoss.divide(pending.getQuantity(), 8, RoundingMode.HALF_UP);
                    stopPrice = longSide ? pending.getEntryPrice().subtract(priceMove) : pending.getEntryPrice().add(priceMove);
                }
            } else {
                stopPrice = longSide ? pending.getEntryPrice().subtract(stopDistance) : pending.getEntryPrice().add(stopDistance);
                possibleLoss = stopPrice.subtract(pending.getEntryPrice()).abs().multiply(pending.getQuantity());
            }
        }
        if (requestedTakeProfitBasis != null) {
            if (takeProfitBasis == TriggerBasis.PNL_PERCENT) {
                if (!canCalculateMarginPnl(pending)) {
                    log.warn("skip pending take profit pnl recalculation id={}, symbol={}, quantity={}, requiredMargin={}, requestedBasis={}",
                            pending.getId(), pending.getSymbol(), pending.getQuantity(), pending.getRequiredMargin(), takeProfitBasis);
                    takeProfitBasis = null;
                } else {
                    possibleProfit = pending.getRequiredMargin().multiply(riskPercent)
                            .divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP);
                    BigDecimal priceMove = possibleProfit.divide(pending.getQuantity(), 8, RoundingMode.HALF_UP);
                    targetPrice = longSide ? pending.getEntryPrice().add(priceMove) : pending.getEntryPrice().subtract(priceMove);
                }
            } else {
                targetPrice = longSide ? pending.getEntryPrice().add(stopDistance) : pending.getEntryPrice().subtract(stopDistance);
                possibleProfit = targetPrice.subtract(pending.getEntryPrice()).abs().multiply(pending.getQuantity());
            }
        }
        return new ExitPlan(stopPrice, targetPrice, possibleLoss, possibleProfit, stopBasis, takeProfitBasis);
    }

    private void normalizePendingPolicyIfNeeded(PendingOrderEntity pending) {
        if (pending != null
                && pending.getMode() == ManagedOrderMode.RAISING_STOP_ONLY
                && pending.hasTakeProfitValues()) {
            pending.clearTakeProfit();
            if (pending.getId() != null) {
                pendingOrderRepository.save(pending);
            }
            log.info("raising pending take profit cleared id={}, symbol={}", pending.getId(), pending.getSymbol());
        }
    }

    private boolean canCalculateMarginPnl(PendingOrderEntity pending) {
        return pending.getQuantity() != null
                && pending.getQuantity().signum() > 0
                && pending.getRequiredMargin() != null
                && pending.getRequiredMargin().signum() > 0;
    }

    private PendingOrderEntity repairPendingSizingIfNeeded(PendingOrderEntity pending) {
        if (pending == null || !pending.hasInvalidSizing()) {
            return pending;
        }
        BigDecimal repairedQuantity = derivePendingQuantity(pending);
        if (!isPositive(repairedQuantity)) {
            log.warn("pending sizing repair skipped id={}, symbol={}, quantity={}, requiredMargin={}, entryPrice={}, stopPrice={}, targetPrice={}, possibleLoss={}, possibleProfit={}",
                    pending.getId(), pending.getSymbol(), pending.getQuantity(), pending.getRequiredMargin(),
                    pending.getEntryPrice(), pending.getStopPrice(), pending.getTargetPrice(),
                    pending.getPossibleLoss(), pending.getPossibleProfit());
            return pending;
        }
        BigDecimal repairedMargin = derivePendingRequiredMargin(pending, repairedQuantity);
        BigDecimal oldQuantity = pending.getQuantity();
        BigDecimal oldRequiredMargin = pending.getRequiredMargin();
        pending.repairSizing(repairedQuantity, repairedMargin);
        PendingOrderEntity saved = pendingOrderRepository.save(pending);
        log.warn("pending sizing repaired id={}, symbol={}, oldQuantity={}, repairedQuantity={}, oldRequiredMargin={}, repairedRequiredMargin={}",
                saved.getId(), saved.getSymbol(), oldQuantity, saved.getQuantity(), oldRequiredMargin, saved.getRequiredMargin());
        return saved;
    }

    private BigDecimal derivePendingQuantity(PendingOrderEntity pending) {
        BigDecimal stopDistance = distance(pending.getEntryPrice(), pending.getStopPrice());
        if (isPositive(pending.getPossibleLoss()) && isPositive(stopDistance)) {
            return pending.getPossibleLoss().divide(stopDistance, 18, RoundingMode.HALF_UP);
        }
        BigDecimal targetDistance = distance(pending.getEntryPrice(), pending.getTargetPrice());
        if (isPositive(pending.getPossibleProfit()) && isPositive(targetDistance)) {
            return pending.getPossibleProfit().divide(targetDistance, 18, RoundingMode.HALF_UP);
        }
        return pending.getQuantity();
    }

    private BigDecimal derivePendingRequiredMargin(PendingOrderEntity pending, BigDecimal quantity) {
        if (pending.getRequiredMargin() != null && pending.getRequiredMargin().signum() > 0) {
            return pending.getRequiredMargin();
        }
        if (isPositive(pending.getEntryPrice()) && isPositive(quantity) && isPositive(pending.getLeverage())) {
            return pending.getEntryPrice().multiply(quantity)
                    .divide(pending.getLeverage(), 18, RoundingMode.HALF_UP);
        }
        return pending.getRequiredMargin();
    }

    private BigDecimal distance(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return null;
        }
        return left.subtract(right).abs();
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }

    @Transactional
    public List<ManagedPositionEntity> activePositions(Long userId) {
        importUnmanagedExchangePositions(userId);
        return managedPositionRepository.findAllByUserIdAndStatusOrderByOpenedAtDesc(userId, ManagedPositionStatus.ACTIVE);
    }

    private boolean isActiveManagedPosition(ManagedPositionEntity position) {
        return position != null
                && (position.getStatus() == ManagedPositionStatus.ACTIVE
                || position.getStatus() == ManagedPositionStatus.ACTIVE_MANAGED);
    }

    private void importUnmanagedExchangePositions(Long userId) {
        List<PositionRisk> exchangePositions;
        try {
            exchangePositions = clientService.getPositions();
        } catch (RuntimeException error) {
            log.warn("unmanaged Binance position import skipped userId={}", userId, error);
            return;
        }
        exchangePositions.stream()
                .filter(position -> position.getPositionAmt() != null && position.getPositionAmt().signum() != 0)
                .forEach(position -> importUnmanagedExchangePosition(userId, position));
    }

    private void importUnmanagedExchangePosition(Long userId, PositionRisk exchangePosition) {
        if (exchangePosition.getSymbol() == null || exchangePosition.getSymbol().isBlank()) {
            return;
        }
        String symbol = exchangePosition.getSymbol().toUpperCase(Locale.ROOT);
        String importKey = userId + ":" + symbol;
        if (!importingExternalPositionKeys.add(importKey)) {
            return;
        }
        try {
            boolean alreadyManaged = managedPositionRepository.existsByUserIdAndSymbolAndStatusInAndExecutionMode(
                    userId,
                    symbol,
                    List.of(ManagedPositionStatus.ACTIVE, ManagedPositionStatus.CLOSING),
                    TradeExecutionMode.REAL
            );
            if (alreadyManaged) {
                return;
            }
            ManagedPositionEntity imported = managedPositionRepository.save(
                    ManagedPositionEntity.fromExternalPosition(userId, exchangePosition)
            );
            multiPlexManager.subscribeMarkPrice(imported.getSymbol());
            monitorService.pushPositionUpdate(imported.getUserId(), imported.getSymbol(), ManagedPositionResponseDto.from(imported));
            log.warn("unmanaged Binance position imported id={}, userId={}, symbol={}, side={}, entryPrice={}, currentPrice={}, quantity={}, leverage={}, mode={}, raiseTrigger={}%, raiseProtection={}%",
                    imported.getId(), userId, imported.getSymbol(), imported.getEntrySide(),
                    imported.getEntryPrice(), imported.getCurrentPrice(), imported.getQuantity(), imported.getLeverage(),
                    imported.getMode(), imported.getRaiseTriggerValue(), imported.getRaiseStopValue());
        } finally {
            importingExternalPositionKeys.remove(importKey);
        }
    }

    public List<ManagedPositionEntity> positionHistory(Long userId, String symbol, String side, ManagedOrderMode mode,
                                                       ManagedPositionCloseReason closeReason) {
        return managedPositionRepository.findAllByUserIdAndStatusInOrderByClosedAtDesc(userId,
                        List.of(ManagedPositionStatus.CLOSED, ManagedPositionStatus.FAILED))
                .stream()
                .filter(position -> symbol == null || symbol.isBlank() || position.getSymbol().equalsIgnoreCase(symbol))
                .filter(position -> side == null || side.isBlank() || position.getEntrySide().equalsIgnoreCase(side)
                        || ("LONG".equalsIgnoreCase(side) && "BUY".equalsIgnoreCase(position.getEntrySide()))
                        || ("SHORT".equalsIgnoreCase(side) && "SELL".equalsIgnoreCase(position.getEntrySide())))
                .filter(position -> mode == null || position.getMode() == mode)
                .filter(position -> closeReason == null || position.getCloseReason() == closeReason)
                .toList();
    }

    public ManagedPositionEntity position(Long id) {
        return managedPositionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("관리 포지션을 찾을 수 없습니다: " + id));
    }

    public ManagedPositionEntity position(Long userId, Long id) {
        ManagedPositionEntity position = position(id);
        assertOwner(position.getUserId(), userId);
        return position;
    }

    @Transactional
    public ManagedPositionEntity updatePositionTriggerBasis(
            Long userId,
            Long id,
            UpdateManagedPositionTriggerBasisRequestDto request
    ) {
        ManagedPositionEntity position = position(userId, id);
        if (position.getStatus() != ManagedPositionStatus.ACTIVE) {
            throw new IllegalArgumentException("ACTIVE 상태의 관리 포지션만 트리거 기준을 변경할 수 있습니다.");
        }
        if (request == null) {
            throw new IllegalArgumentException("변경할 트리거 기준이 필요합니다.");
        }
        boolean raisingStopOnly = position.getMode() == ManagedOrderMode.RAISING_STOP_ONLY;
        if (request.getStopTriggerBasis() == null
                && (raisingStopOnly || request.getTakeProfitTriggerBasis() == null)) {
            throw new IllegalArgumentException("변경할 SL 또는 TP 트리거 기준이 필요합니다.");
        }

        TriggerBasis previousStopBasis = position.getStopTriggerBasis();
        TriggerBasis previousTakeProfitBasis = position.getTakeProfitTriggerBasis();
        position.updateTriggerBasis(
                request.getStopTriggerBasis(),
                raisingStopOnly ? null : request.getTakeProfitTriggerBasis()
        );
        ManagedPositionEntity saved = managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(saved.getUserId(), saved.getSymbol(), ManagedPositionResponseDto.from(saved));
        log.info("managed position trigger basis updated id={}, symbol={}, mode={}, previousStopBasis={}, stopBasis={}, previousTakeProfitBasis={}, takeProfitBasis={}, currentStopPrice={}, targetPrice={}, possibleLoss={}, possibleProfit={}",
                saved.getId(), saved.getSymbol(), saved.getMode(), previousStopBasis, saved.getStopTriggerBasis(),
                previousTakeProfitBasis, saved.getTakeProfitTriggerBasis(), saved.getCurrentStopPrice(),
                saved.getTargetPrice(), saved.getPossibleLoss(), saved.getPossibleProfit());
        return saved;
    }

    @Transactional
    public ManagedPositionEntity addRaisingStop(Long userId, Long id, AddRaisingStopRequestDto request) {
        ManagedPositionEntity position = position(userId, id);
        if (request == null) {
            throw new IllegalArgumentException("손절선 상승 설정이 필요합니다.");
        }
        if (position.getStatus() != ManagedPositionStatus.ACTIVE) {
            throw new IllegalArgumentException("ACTIVE 상태의 관리 포지션만 손절선 상승 모드를 추가할 수 있습니다.");
        }
        TriggerBasis previousStopBasis = position.getStopTriggerBasis();
        BigDecimal previousTargetPrice = position.getTargetPrice();
        position.addRaisingStop(
                request.getRaiseTriggerBasis(),
                positiveOrNull(request.getRaiseTriggerValue(), "raiseTriggerValue"),
                request.getRaiseStopType(),
                positiveOrNull(request.getRaiseStopValue(), "raiseStopValue")
        );
        ManagedPositionEntity saved = managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(saved.getUserId(), saved.getSymbol(), ManagedPositionResponseDto.from(saved));
        log.info("managed position raising stop applied id={}, symbol={}, mode={}, previousStopBasis={}, stopBasis={}, previousTargetPrice={}, currentStopPrice={}, raiseTriggerValue={}, raiseStopType={}, raiseStopValue={}",
                saved.getId(), saved.getSymbol(), saved.getMode(), previousStopBasis, saved.getStopTriggerBasis(),
                previousTargetPrice, saved.getCurrentStopPrice(), saved.getRaiseTriggerValue(),
                saved.getRaiseStopType(), saved.getRaiseStopValue());
        return saved;
    }

    @Transactional
    public ManagedPositionEntity updatePositionMode(Long userId, Long id, UpdateManagedPositionModeRequestDto request) {
        if (request == null || request.getMode() == null) {
            throw new IllegalArgumentException("변경할 모드가 필요합니다.");
        }
        ManagedPositionEntity position = position(userId, id);
        if (position.getStatus() != ManagedPositionStatus.ACTIVE) {
            throw new IllegalArgumentException("ACTIVE 상태의 관리 포지션만 모드를 변경할 수 있습니다.");
        }
        ManagedOrderMode previousMode = position.getMode();
        if (request.getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            position.addRaisingStop(
                    request.getRaiseTriggerBasis(),
                    positiveOrNull(request.getRaiseTriggerValue(), "raiseTriggerValue"),
                    request.getRaiseStopType(),
                    positiveOrNull(request.getRaiseStopValue(), "raiseStopValue")
            );
        } else {
            position.switchToFixedTpSl(
                    request.getStopTriggerBasis(),
                    request.getStopValueType(),
                    request.getStopValue(),
                    request.getTakeProfitTriggerBasis(),
                    request.getTakeProfitValueType(),
                    request.getTakeProfitValue()
            );
        }
        ManagedPositionEntity saved = managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(saved.getUserId(), saved.getSymbol(), ManagedPositionResponseDto.from(saved));
        log.info("managed position mode updated id={}, symbol={}, previousMode={}, mode={}, currentStopPrice={}, targetPrice={}, raiseTriggerValue={}, raiseStopType={}, raiseStopValue={}",
                saved.getId(), saved.getSymbol(), previousMode, saved.getMode(), saved.getCurrentStopPrice(),
                saved.getTargetPrice(), saved.getRaiseTriggerValue(), saved.getRaiseStopType(), saved.getRaiseStopValue());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ManagedPositionJournalEntity> journals(Long userId, String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return managedPositionJournalRepository.findAllByManagedPosition_UserIdOrderByUpdatedAtDesc(userId);
        }
        return managedPositionJournalRepository.findAllByManagedPosition_UserIdAndManagedPosition_SymbolIgnoreCaseOrderByUpdatedAtDesc(userId, symbol);
    }

    @Transactional(readOnly = true)
    public ManagedPositionJournalEntity journal(Long userId, Long positionId) {
        return managedPositionJournalRepository.findByManagedPosition_IdAndManagedPosition_UserId(positionId, userId)
                .orElseThrow(() -> new IllegalArgumentException("매매일지를 찾을 수 없습니다: " + positionId));
    }

    @Transactional
    public ManagedPositionJournalEntity upsertJournal(Long positionId, Long userId,
                                                      ManagedPositionJournalRequestDto request) {
        ManagedPositionEntity position = position(userId, positionId);
        if (position.getStatus() == ManagedPositionStatus.ACTIVE || position.getStatus() == ManagedPositionStatus.CLOSING) {
            throw new IllegalArgumentException("종료된 포지션에만 매매일지를 작성할 수 있습니다.");
        }
        ManagedPositionJournalEntity journal = managedPositionJournalRepository.findByManagedPosition_Id(positionId)
                .orElseGet(() -> ManagedPositionJournalEntity.create(
                        position,
                        userId,
                        request.getTitle(),
                        request.getEntryReason(),
                        request.getContent(),
                        request.getReview(),
                        request.getTags()
                ));
        journal.update(
                request.getTitle(),
                request.getEntryReason(),
                request.getContent(),
                request.getReview(),
                request.getTags()
        );
        ManagedPositionJournalEntity saved = managedPositionJournalRepository.save(journal);
        position.appendEvent("JOURNAL_SAVED journalId=" + saved.getId());
        managedPositionRepository.save(position);
        return saved;
    }

    @Transactional
    public void deleteJournal(Long userId, Long positionId) {
        managedPositionJournalRepository.findByManagedPosition_IdAndManagedPosition_UserId(positionId, userId)
                .ifPresent(managedPositionJournalRepository::delete);
    }

    @Transactional
    public ManagedPositionEntity closePosition(Long userId, Long id) {
        ManagedPositionEntity position = position(userId, id);
        closePosition(position, ManagedPositionCloseReason.MANUAL_CLOSE);
        return position(position.getId());
    }

    public ManagedPositionEntity startTestVolatility(Long userId, Long id, String direction) {
        ManagedPositionEntity position = position(userId, id);
        if (!position.isTestOrder()) {
            throw new IllegalArgumentException("테스트 포지션만 변동성 테스트를 실행할 수 있습니다.");
        }
        if (!isActiveManagedPosition(position)) {
            throw new IllegalArgumentException("ACTIVE 상태의 테스트 포지션만 변동성 테스트를 실행할 수 있습니다.");
        }
        boolean upward = "UP".equalsIgnoreCase(direction) || "UPWARD".equalsIgnoreCase(direction);
        boolean downward = "DOWN".equalsIgnoreCase(direction) || "DOWNWARD".equalsIgnoreCase(direction);
        if (!upward && !downward) {
            throw new IllegalArgumentException("direction은 UP 또는 DOWN만 지원합니다.");
        }
        ScheduledFuture<?> previous = testVolatilityTasks.remove(position.getId());
        if (previous != null) {
            previous.cancel(false);
        }
        ScheduledFuture<?> task = testVolatilityExecutor.scheduleAtFixedRate(
                () -> emitTestVolatilityTick(position.getId(), upward),
                0,
                1,
                TimeUnit.SECONDS
        );
        testVolatilityTasks.put(position.getId(), task);
        log.info("test volatility started positionId={}, symbol={}, direction={}, basePrice={}",
                position.getId(), position.getSymbol(), upward ? "UP" : "DOWN",
                position.getCurrentPrice() == null ? position.getEntryPrice() : position.getCurrentPrice());
        return position;
    }

    public ManagedPositionEntity stopTestVolatility(Long userId, Long id) {
        ManagedPositionEntity position = position(userId, id);
        stopTestVolatility(position.getId());
        log.info("test volatility stopped positionId={}, symbol={}", position.getId(), position.getSymbol());
        return position(position.getId());
    }

    private void emitTestVolatilityTick(Long positionId, boolean upward) {
        try {
            ManagedPositionEntity position = managedPositionRepository.findById(positionId).orElse(null);
            if (position == null || !position.isTestOrder() || !isActiveManagedPosition(position)) {
                stopTestVolatility(positionId);
                return;
            }
            BigDecimal basePrice = position.getCurrentPrice() == null || position.getCurrentPrice().signum() <= 0
                    ? position.getEntryPrice()
                    : position.getCurrentPrice();
            if (basePrice == null || basePrice.signum() <= 0) {
                stopTestVolatility(positionId);
                return;
            }
            BigDecimal percent = BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(0.1, 1.0))
                    .divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
            BigDecimal nextPrice = upward
                    ? basePrice.multiply(BigDecimal.ONE.add(percent))
                    : basePrice.multiply(BigDecimal.ONE.subtract(percent));
            log.info("test volatility tick positionId={}, symbol={}, direction={}, basePrice={}, nextPrice={}, movePercent={}",
                    positionId, position.getSymbol(), upward ? "UP" : "DOWN", basePrice, nextPrice,
                    percent.multiply(new BigDecimal("100")));
            evaluatePositionOnce(position, nextPrice);
            ManagedPositionEntity latest = managedPositionRepository.findById(positionId).orElse(null);
            if (latest == null || !isActiveManagedPosition(latest)) {
                stopTestVolatility(positionId);
            }
        } catch (RuntimeException error) {
            stopTestVolatility(positionId);
            log.warn("test volatility stopped by error positionId={}", positionId, error);
        }
    }

    private void stopTestVolatility(Long positionId) {
        ScheduledFuture<?> task = testVolatilityTasks.remove(positionId);
        if (task != null) {
            task.cancel(false);
        }
    }

    @Transactional(readOnly = true)
    public List<ManagedStopHistoryEntity> stopHistory(Long userId, Long positionId) {
        position(userId, positionId);
        return managedStopHistoryRepository
                .findAllByManagedPosition_IdAndUserIdOrderByChangedAtDesc(positionId, userId);
    }

    private void assertOwner(Long ownerUserId, Long sessionUserId) {
        if (ownerUserId == null || sessionUserId == null || !ownerUserId.equals(sessionUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "요청한 사용자의 데이터가 아닙니다.");
        }
    }

    @EventListener
    @Transactional
    public void handleOrderTradeUpdate(OrderTradeUpdateEvent event) {
        if (event == null || (isBlank(event.orderId()) && isBlank(event.clientOrderId()))) {
            return;
        }
        boolean terminalUpdate = event.isFilled() || event.isCanceled() || event.isExpired();
        if (terminalUpdate) {
            bufferUnmatchedOrderUpdate(event);
        }
        findPendingOrder(event).ifPresentOrElse(pending -> {
            removeBufferedOrderUpdate(pending);
            handlePendingOrderUpdate(pending, event);
        }, () -> {
            if (terminalUpdate) {
                log.warn("Binance order update arrived before managed pending order was visible orderId={}, clientOrderId={}, status={}",
                        event.orderId(), event.clientOrderId(), event.orderStatus());
            }
        });
        if (!isBlank(event.orderId())) {
            managedPositionRepository.findFirstByCloseOrderId(event.orderId())
                    .ifPresent(position -> handleCloseOrderUpdate(position, event));
        }
    }

    private java.util.Optional<PendingOrderEntity> findPendingOrder(OrderTradeUpdateEvent event) {
        if (!isBlank(event.orderId())) {
            java.util.Optional<PendingOrderEntity> byOrderId = pendingOrderRepository.findFirstByOrderId(event.orderId());
            if (byOrderId.isPresent()) {
                return byOrderId;
            }
        }
        if (!isBlank(event.clientOrderId())) {
            return pendingOrderRepository.findFirstByClientOrderId(event.clientOrderId());
        }
        return java.util.Optional.empty();
    }

    private void bufferUnmatchedOrderUpdate(OrderTradeUpdateEvent event) {
        trimUnmatchedOrderUpdates();
        if (!isBlank(event.orderId())) {
            unmatchedOrderUpdates.put(orderUpdateKey("ORDER", event.orderId()), event);
        }
        if (!isBlank(event.clientOrderId())) {
            unmatchedOrderUpdates.put(orderUpdateKey("CLIENT", event.clientOrderId()), event);
        }
    }

    private void trimUnmatchedOrderUpdates() {
        while (unmatchedOrderUpdates.size() >= MAX_UNMATCHED_ORDER_UPDATE_KEYS) {
            String key = unmatchedOrderUpdates.keySet().stream().findFirst().orElse(null);
            if (key == null) {
                return;
            }
            unmatchedOrderUpdates.remove(key);
        }
    }

    private OrderTradeUpdateEvent removeBufferedOrderUpdate(PendingOrderEntity pending) {
        OrderTradeUpdateEvent event = null;
        if (!isBlank(pending.getOrderId())) {
            event = unmatchedOrderUpdates.remove(orderUpdateKey("ORDER", pending.getOrderId()));
        }
        if (!isBlank(pending.getClientOrderId())) {
            OrderTradeUpdateEvent byClientOrderId = unmatchedOrderUpdates.remove(orderUpdateKey("CLIENT", pending.getClientOrderId()));
            if (event == null) {
                event = byClientOrderId;
            }
        }
        return event;
    }

    private String orderUpdateKey(String type, String value) {
        return type + ":" + value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    @EventListener
    @Transactional
    public void handlePriceTick(PriceTickEvent event) {
        if (event == null || event.price() == null) {
            return;
        }
        pendingOrderRepository.findAllBySymbolAndStatusAndExecutionMode(
                        event.symbol(), PendingOrderStatus.PENDING, TradeExecutionMode.TEST)
                .stream()
                .filter(pending -> testEntryReached(pending, event.price()))
                .forEach(pending -> openTestPosition(pending, event.price()));
        managedPositionRepository.findAllByStatusInOrderByOpenedAtDesc(
                        List.of(ManagedPositionStatus.ACTIVE, ManagedPositionStatus.ACTIVE_MANAGED))
                .stream()
                .filter(position -> event.symbol().equalsIgnoreCase(position.getSymbol()))
                .filter(position -> !isRunningTestVolatility(position))
                .forEach(position -> evaluatePositionOnce(position, event.price()));
    }

    private boolean isRunningTestVolatility(ManagedPositionEntity position) {
        return position != null
                && position.isTestOrder()
                && position.getId() != null
                && testVolatilityTasks.containsKey(position.getId());
    }

    private void evaluatePositionOnce(ManagedPositionEntity position, BigDecimal currentPrice) {
        if (position == null || position.getId() == null) {
            log.debug("managed close skipped positionId={}, closeSkippedReason=POSITION_NOT_ACTIVE",
                    position == null ? null : position.getId());
            return;
        }
        if (closingPositionIds.contains(position.getId())) {
            log.debug("managed close skipped positionId={}, symbol={}, closeSkippedReason=ALREADY_CLOSING",
                    position.getId(), position.getSymbol());
            return;
        }
        if (!evaluatingPositionIds.add(position.getId())) {
            log.debug("managed close skipped positionId={}, symbol={}, closeSkippedReason=EVALUATION_IN_PROGRESS",
                    position.getId(), position.getSymbol());
            return;
        }
        try {
            ManagedPositionEntity latest = managedPositionRepository.findById(position.getId()).orElse(null);
            if (latest == null || !isActiveManagedPosition(latest)) {
                log.debug("managed close skipped positionId={}, symbol={}, status={}, closeSkippedReason=POSITION_NOT_ACTIVE",
                        position.getId(), position.getSymbol(), latest == null ? null : latest.getStatus());
                return;
            }
            if (closingPositionIds.contains(latest.getId())) {
                log.debug("managed close skipped positionId={}, symbol={}, closeSkippedReason=ALREADY_CLOSING",
                        latest.getId(), latest.getSymbol());
                return;
            }
            evaluatePosition(latest, currentPrice);
        } finally {
            evaluatingPositionIds.remove(position.getId());
        }
    }

    private boolean testEntryReached(PendingOrderEntity pending, BigDecimal currentPrice) {
        return "BUY".equalsIgnoreCase(pending.getSide())
                ? currentPrice.compareTo(pending.getEntryPrice()) <= 0
                : currentPrice.compareTo(pending.getEntryPrice()) >= 0;
    }

    private void openTestPosition(PendingOrderEntity pending, BigDecimal currentPrice) {
        if (pending.getStatus() != PendingOrderStatus.PENDING
                || !pending.isTestOrder()
                || !fillingTestOrderIds.add(pending.getId())) {
            return;
        }
        try {
            ManagedPositionEntity position = fillPendingOrder(pending, pending.getEntryPrice(), pending.getQuantity(), "TEST_MARK_PRICE");
            if (position != null) {
                position.appendEvent("TEST_ENTRY_FILLED limitPrice=" + pending.getEntryPrice()
                        + ", triggerMarkPrice=" + currentPrice + ", BinanceOrderApiCalled=false");
                managedPositionRepository.save(position);
                log.info("managed test order filled internally pendingId={}, positionId={}, symbol={}, configuredEntryPrice={}, fillPrice={}, quantity={}",
                        pending.getId(), position.getId(), position.getSymbol(), pending.getEntryPrice(),
                        position.getEntryPrice(), position.getQuantity());
            }
        } finally {
            fillingTestOrderIds.remove(pending.getId());
        }
    }

    private void handlePendingOrderUpdate(PendingOrderEntity pending, OrderTradeUpdateEvent event) {
        if (pending.isTestOrder()) {
            log.warn("ignored exchange event for test order id={}, orderId={}", pending.getId(), pending.getOrderId());
            return;
        }
        if (pending.getStatus() != PendingOrderStatus.PENDING) {
            return;
        }
        if (event.isFilled()) {
            fillPendingOrder(pending, event.averagePrice(), event.executedQuantity(), "BINANCE_USER_STREAM");
            return;
        }
        if (event.isCanceled()) {
            pending.mark(PendingOrderStatus.CANCELED);
            pendingOrderRepository.save(pending);
            return;
        }
        if (event.isExpired()) {
            pending.mark(PendingOrderStatus.EXPIRED);
            pendingOrderRepository.save(pending);
        }
    }

    private ManagedPositionEntity fillPendingOrder(PendingOrderEntity pending, BigDecimal fillPrice,
                                                   BigDecimal executedQuantity, String source) {
        if (pending == null || pending.getId() == null || !fillingPendingOrderIds.add(pending.getId())) {
            return null;
        }
        try {
            PendingOrderEntity latest = pendingOrderRepository.findById(pending.getId()).orElse(null);
            if (latest == null || latest.getStatus() != PendingOrderStatus.PENDING) {
                return null;
            }
            latest = repairPendingSizingIfNeeded(latest);
            latest.mark(PendingOrderStatus.FILLED);
            pendingOrderRepository.save(latest);
            ManagedPositionEntity position = openManagedPosition(latest, fillPrice, executedQuantity);
            position.appendEvent("MANAGED_POSITION_OPENED source=" + source);
            managedPositionRepository.save(position);
            monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
            log.info("managed position opened source={}, executionMode={}, id={}, symbol={}, entryOrderId={}, fillPrice={}, executedQuantity={}, leverage={}, requiredMargin={}",
                    source, position.getExecutionMode(), position.getId(), position.getSymbol(),
                    position.getEntryOrderId(), position.getEntryPrice(), position.getQuantity(),
                    position.getLeverage(), position.getRequiredMargin());
            return position;
        } finally {
            fillingPendingOrderIds.remove(pending.getId());
        }
    }

    private ManagedPositionEntity openManagedPosition(PendingOrderEntity pending, BigDecimal fillPrice,
                                                      BigDecimal executedQuantity) {
        ManagedPositionEntity position = managedPositionRepository.save(
                ManagedPositionEntity.from(pending, fillPrice, executedQuantity)
        );
        multiPlexManager.subscribeMarkPrice(position.getSymbol());
        return position;
    }

    private void handleCloseOrderUpdate(ManagedPositionEntity position, OrderTradeUpdateEvent event) {
        if (position.getStatus() != ManagedPositionStatus.CLOSING || !event.isFilled()) {
            return;
        }
        BigDecimal closePrice = event.averagePrice().signum() == 0
                ? position.getCurrentPrice()
                : event.averagePrice();
        BigDecimal realized = calculatePnl(position, closePrice);
        position.markClosed(realized, closePrice);
        closingPositionIds.remove(position.getId());
        managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
        log.info("managed position closed id={}, reason={}, realizedPnl={}",
                position.getId(), position.getCloseReason(), realized);
    }

    private void evaluatePosition(ManagedPositionEntity position, BigDecimal currentPrice) {
        if (!isActiveManagedPosition(position)) {
            logRaiseStopEvaluation(position, currentPrice, null, null, null,
                    false, false, "POSITION_NOT_ACTIVE");
            return;
        }
        if (position.isLegacyManagedPolicy()) {
            log.warn("legacy managed position found during price event id={}, symbol={}, mode={}, initialStop={}, currentStop={}, target={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), position.getTargetPrice());
            migrateLegacyPositionIfNeeded(position);
            position = position(position.getId());
        }
        BigDecimal pnl = calculatePnl(position, currentPrice);
        BigDecimal marginPnlPercent = ManagedTradeRiskPolicy.marginPnlPercent(pnl, position.getRequiredMargin());
        BigDecimal highest = max(position.getHighestPrice(), currentPrice);
        BigDecimal lowest = min(position.getLowestPrice(), currentPrice);
        BigDecimal nextStop = position.getCurrentStopPrice();
        BigDecimal previousStop = position.getCurrentStopPrice();
        boolean raiseActivated = position.isRaiseActivated();
        ManagedRaiseStopCalculator.RaiseStopPlan raisePlan = null;
        boolean triggerReached = false;
        boolean stopUpdateApplied = false;
        String stopUpdateRejectedReason = "MODE_NOT_RAISING_STOP";

        ManagedPositionCloseReason reason = fixedCloseReason(position, currentPrice, pnl);
        if (position.getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            log.debug("raising stop inspect id={}, symbol={}, mode={}, initialStop={}, currentStop={}, price={}, pnl={}, raiseActivated={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), currentPrice, pnl, raiseActivated);
            raisePlan = ManagedRaiseStopCalculator.calculate(position, currentPrice, pnl);
            if (raisePlan == null) {
                stopUpdateRejectedReason = "INVALID_CALCULATION";
            } else {
                triggerReached = ManagedRaiseStopCalculator.reached(position, currentPrice, pnl, raisePlan);
                stopUpdateRejectedReason = triggerReached
                        ? "NOT_BETTER_THAN_CURRENT_STOP"
                        : "TRIGGER_NOT_REACHED";
            }
            if (triggerReached && isBetterStop(position, raisePlan.nextStopPrice())) {
                raiseActivated = true;
                nextStop = raisePlan.nextStopPrice();
                stopUpdateApplied = true;
                stopUpdateRejectedReason = null;
                log.info("managed raise trigger reached id={}, symbol={}, basis={}, currentPrice={}, currentPnl={}, triggerPrice={}, triggerPnl={}, previousStop={}, nextStop={}",
                        position.getId(), position.getSymbol(), position.getStopTriggerBasis(), currentPrice, pnl,
                        raisePlan.triggerPrice(), raisePlan.triggerPnl(), position.getCurrentStopPrice(), nextStop);
            }
            reason = raisingCloseReason(position, currentPrice, pnl, nextStop, raiseActivated);
        }
        if (position.getMode() != ManagedOrderMode.RAISING_STOP_ONLY && nextStop.compareTo(position.getCurrentStopPrice()) != 0) {
            log.warn("fixed position stop mutation blocked id={}, symbol={}, mode={}, initialStop={}, currentStop={}, attemptedStop={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), nextStop);
            nextStop = position.getCurrentStopPrice();
            raiseActivated = false;
            stopUpdateApplied = false;
            stopUpdateRejectedReason = "MODE_NOT_RAISING_STOP";
        }
        if (stopUpdateApplied && nextStop.compareTo(position.getCurrentStopPrice()) != 0) {
            log.info("managed stop changed id={}, symbol={}, mode={}, initialStop={}, previousStop={}, nextStop={}, price={}, pnl={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), nextStop, currentPrice, pnl);
            position.appendEvent("STOP_CHANGED previousStop=" + position.getCurrentStopPrice()
                    + ", nextStop=" + nextStop + ", price=" + currentPrice + ", pnl=" + pnl);
        }

        position.updateMarket(currentPrice, pnl, highest, lowest, nextStop, raiseActivated);
        managedPositionRepository.save(position);
        if (stopUpdateApplied) {
            ManagedStopUpdateReason updateReason = position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT
                    ? ManagedStopUpdateReason.PNL_TRIGGER_REACHED
                    : ManagedStopUpdateReason.PRICE_TRIGGER_REACHED;
            recordStopUpdate(position, currentPrice, previousStop, nextStop, pnl, raisePlan, updateReason);
        }
        monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
        logRaiseStopEvaluation(position, currentPrice, pnl, raisePlan, nextStop,
                triggerReached, stopUpdateApplied, stopUpdateRejectedReason);
        boolean stopConditionMatched = reason == ManagedPositionCloseReason.STOP_PRICE_REACHED
                || reason == ManagedPositionCloseReason.POSSIBLE_LOSS_REACHED
                || reason == ManagedPositionCloseReason.RAISING_STOP_REACHED;
        String closeSkippedReason = reason == null ? closeSkipReason(position) : null;
        log.debug("managed position evaluated positionId={}, symbol={}, side={}, triggerBasis={}, entryPrice={}, currentPrice={}, quantity={}, requiredMargin={}, leverage={}, unrealizedPnl={}, marginPnlPercent={}, stopPercent={}, possibleLoss={}, currentStopPrice={}, stopConditionMatched={}, closeRequested={}, closeSkippedReason={}",
                position.getId(), position.getSymbol(), position.getEntrySide(), position.getStopTriggerBasis(),
                position.getEntryPrice(), currentPrice, position.getQuantity(), position.getRequiredMargin(),
                position.getLeverage(), pnl, marginPnlPercent, position.getRiskPercent(),
                position.getPossibleLoss(), nextStop, stopConditionMatched, reason != null, closeSkippedReason);
        if (reason != null) {
            position.appendEvent("CLOSE_CONDITION_REACHED reason=" + reason + ", price=" + currentPrice
                    + ", stop=" + nextStop + ", pnl=" + pnl);
            log.info("managed close triggered id={}, symbol={}, mode={}, reason={}, entryPrice={}, currentPrice={}, currentStop={}, pnl={}, raiseActivated={}, stopBasis={}, raiseTriggerType={}, raiseTriggerValue={}, raiseStopType={}, raiseStopValue={}",
                    position.getId(), position.getSymbol(), position.getMode(), reason, position.getEntryPrice(),
                    currentPrice, nextStop, pnl, raiseActivated, position.getStopTriggerBasis(),
                    position.getRaiseTriggerType(), position.getRaiseTriggerValue(),
                    position.getRaiseStopType(), position.getRaiseStopValue());
            closePosition(position, reason);
        }
    }

    private boolean isBetterStop(ManagedPositionEntity position, BigDecimal candidate) {
        if (candidate == null || position.getCurrentStopPrice() == null) {
            return false;
        }
        return isLong(position)
                ? candidate.compareTo(position.getCurrentStopPrice()) > 0
                : candidate.compareTo(position.getCurrentStopPrice()) < 0;
    }

    private void recordStopUpdate(
            ManagedPositionEntity position,
            BigDecimal currentPrice,
            BigDecimal previousStop,
            BigDecimal newStop,
            BigDecimal pnl,
            ManagedRaiseStopCalculator.RaiseStopPlan plan,
            ManagedStopUpdateReason reason
    ) {
        if (position.getId() == null
                || previousStop == null
                || newStop == null
                || previousStop.compareTo(newStop) == 0) {
            return;
        }
        String fingerprint = position.getId() + ":" + newStop.stripTrailingZeros().toPlainString();
        if (!recordedStopUpdates.add(fingerprint)
                || managedStopHistoryRepository.existsByManagedPosition_IdAndNewStopPrice(position.getId(), newStop)) {
            return;
        }
        try {
            managedStopHistoryRepository.save(ManagedStopHistoryEntity.create(
                    position,
                    currentPrice,
                    previousStop,
                    newStop,
                    pnl == null ? BigDecimal.ZERO : pnl,
                    pnl == null ? BigDecimal.ZERO : pnlPercent(position, pnl),
                    pricePercent(position, currentPrice),
                    plan,
                    reason
            ));
        } catch (RuntimeException e) {
            recordedStopUpdates.remove(fingerprint);
            throw e;
        }
    }

    private void logRaiseStopEvaluation(
            ManagedPositionEntity position,
            BigDecimal currentPrice,
            BigDecimal pnl,
            ManagedRaiseStopCalculator.RaiseStopPlan plan,
            BigDecimal evaluatedStop,
            boolean triggerReached,
            boolean stopUpdateApplied,
            String rejectedReason
    ) {
        log.debug("managed raise stop evaluated positionId={}, side={}, entryPrice={}, currentPrice={}, currentStopPrice={}, triggerBasis={}, triggerValue={}, triggerPrice={}, triggerReached={}, protectType={}, protectValue={}, expectedNextStopPrice={}, stopUpdateApplied={}, stopUpdateRejectedReason={}, currentPnl={}, currentPnlPercent={}, currentPriceChangePercent={}, evaluatedStop={}",
                position.getId(), position.getEntrySide(), position.getEntryPrice(), currentPrice,
                position.getCurrentStopPrice(), position.getStopTriggerBasis(), position.getRaiseTriggerValue(),
                plan == null ? null : plan.triggerPrice(), triggerReached, position.getRaiseStopType(),
                position.getRaiseStopValue(), plan == null ? null : plan.nextStopPrice(), stopUpdateApplied,
                rejectedReason, pnl, pnl == null ? null : pnlPercent(position, pnl),
                currentPrice == null ? null : pricePercent(position, currentPrice), evaluatedStop);
    }

    private ManagedPositionCloseReason fixedCloseReason(ManagedPositionEntity position, BigDecimal price, BigDecimal pnl) {
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            if (ManagedTradeRiskPolicy.pnlStopMatched(
                    pnl,
                    position.getRequiredMargin(),
                    position.getRiskPercent(),
                    position.getPossibleLoss()
            )) {
                return ManagedPositionCloseReason.POSSIBLE_LOSS_REACHED;
            }
        } else if (isLong(position)) {
            if (price.compareTo(position.getCurrentStopPrice()) <= 0) return ManagedPositionCloseReason.STOP_PRICE_REACHED;
        } else if (price.compareTo(position.getCurrentStopPrice()) >= 0) {
            return ManagedPositionCloseReason.STOP_PRICE_REACHED;
        }

        if (position.getTakeProfitTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            if (position.getPossibleProfit() != null && pnl.compareTo(position.getPossibleProfit()) >= 0) {
                return ManagedPositionCloseReason.POSSIBLE_PROFIT_REACHED;
            }
        } else if (isLong(position)) {
            if (position.getTargetPrice() != null && price.compareTo(position.getTargetPrice()) >= 0) {
                return ManagedPositionCloseReason.TARGET_PRICE_REACHED;
            }
        } else if (position.getTargetPrice() != null && price.compareTo(position.getTargetPrice()) <= 0) {
            return ManagedPositionCloseReason.TARGET_PRICE_REACHED;
        }
        return null;
    }

    private ManagedPositionCloseReason raisingCloseReason(ManagedPositionEntity position, BigDecimal price,
                                                          BigDecimal pnl, BigDecimal stop, boolean activated) {
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            if (!activated && (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0
                    || position.getQuantity() == null || position.getQuantity().signum() <= 0)) {
                log.error("managed close evaluation skipped because possibleLoss is invalid id={}, symbol={}, possibleLoss={}, requiredMargin={}, quantity={}",
                        position.getId(), position.getSymbol(), position.getPossibleLoss(),
                        position.getRequiredMargin(), position.getQuantity());
                return null;
            }
            boolean matched = activated
                    ? pnl.compareTo(calculatePnl(position, stop)) <= 0
                    : ManagedTradeRiskPolicy.pnlStopMatched(
                            pnl,
                            position.getRequiredMargin(),
                            position.getRiskPercent(),
                            position.getPossibleLoss()
                    );
            if (matched) {
                return activated ? ManagedPositionCloseReason.RAISING_STOP_REACHED : ManagedPositionCloseReason.POSSIBLE_LOSS_REACHED;
            }
            return null;
        }
        if (isLong(position) && price.compareTo(stop) <= 0) {
            return activated ? ManagedPositionCloseReason.RAISING_STOP_REACHED : ManagedPositionCloseReason.STOP_PRICE_REACHED;
        }
        if (!isLong(position) && price.compareTo(stop) >= 0) {
            return activated ? ManagedPositionCloseReason.RAISING_STOP_REACHED : ManagedPositionCloseReason.STOP_PRICE_REACHED;
        }
        return null;
    }

    private void closePosition(ManagedPositionEntity position, ManagedPositionCloseReason reason) {
        if (!isActiveManagedPosition(position)) {
            log.info("managed close skipped positionId={}, symbol={}, status={}, closeSkippedReason=POSITION_NOT_ACTIVE",
                    position.getId(), position.getSymbol(), position.getStatus());
            return;
        }
        if (!closingPositionIds.add(position.getId())) {
            log.info("managed close skipped positionId={}, symbol={}, closeSkippedReason=ALREADY_CLOSING",
                    position.getId(), position.getSymbol());
            return;
        }
        if (position.isTestOrder()) {
            BigDecimal closePrice = position.getCurrentPrice() == null ? position.getEntryPrice() : position.getCurrentPrice();
            position.markTestClosed(reason, calculatePnl(position, closePrice), closePrice);
            closingPositionIds.remove(position.getId());
            managedPositionRepository.save(position);
            monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
            log.info("managed test position closed internally id={}, symbol={}, reason={}, closePrice={}, realizedPnl={}",
                    position.getId(), position.getSymbol(), reason, closePrice, position.getRealizedPnl());
            return;
        }
        position.markClosing(null, reason);
        managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
        try {
            BigDecimal closeQuantity = actualCloseQuantityOrCloseAsExternal(position);
            if (closeQuantity == null) {
                closingPositionIds.remove(position.getId());
                return;
            }
            Map<String, String> closeParams = closeCurrentPriceParams(position, closeQuantity);
            log.info("managed close order sending positionId={}, oldStatus=ACTIVE, newStatus=CLOSING, symbol={}, quantity={}, reduceOnly={}, closeReason={}",
                    position.getId(), position.getSymbol(), closeParams.get("quantity"),
                    closeParams.get("reduceOnly"), reason);
            Order order = exchangeConnector.order(closeParams);
            position.markClosing(order.getOrderId(), reason);
            managedPositionRepository.save(position);
            monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
        } catch (RuntimeException e) {
            if (closeRejectedBecauseAlreadyClosed(position, e)) {
                closingPositionIds.remove(position.getId());
                return;
            }
            position.restoreActiveAfterCloseFailure(e.getMessage());
            closingPositionIds.remove(position.getId());
            managedPositionRepository.save(position);
            monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
            log.error("managed close failed positionId={}, symbol={}, errorCode={}, errorMessage={}, closeSkippedReason=ORDER_FAILED",
                    position.getId(), position.getSymbol(), binanceErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    private BigDecimal actualCloseQuantityOrCloseAsExternal(ManagedPositionEntity position) {
        PositionRisk actual = currentExchangePosition(position);
        if (actual == null
                || actual.getPositionAmt() == null
                || actual.getPositionAmt().signum() == 0
                || !sameDirection(position, actual)) {
            BigDecimal closePrice = position.getCurrentPrice() == null ? position.getEntryPrice() : position.getCurrentPrice();
            position.markClosing(null, ManagedPositionCloseReason.EXTERNAL_POSITION_CLOSED);
            position.markClosed(calculatePnl(position, closePrice), closePrice);
            managedPositionRepository.save(position);
            monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
            log.warn("managed close skipped positionId={}, symbol={}, closeSkippedReason=POSITION_ALREADY_CLOSED",
                    position.getId(), position.getSymbol());
            return null;
        }
        return actual.getPositionAmt().abs();
    }

    private boolean closeRejectedBecauseAlreadyClosed(ManagedPositionEntity position, RuntimeException error) {
        String message = error.getMessage() == null ? "" : error.getMessage();
        if (!message.contains("-2022") && !message.contains("ReduceOnly")) {
            return false;
        }
        PositionRisk actual;
        try {
            actual = currentExchangePosition(position);
        } catch (RuntimeException checkError) {
            log.warn("managed close rejection recovery skipped because actual position check failed positionId={}, symbol={}",
                    position.getId(), position.getSymbol(), checkError);
            return false;
        }
        if (actual != null && actual.getPositionAmt() != null && actual.getPositionAmt().signum() != 0
                && sameDirection(position, actual)) {
            return false;
        }
        BigDecimal closePrice = position.getCurrentPrice() == null ? position.getEntryPrice() : position.getCurrentPrice();
        position.markClosing(null, ManagedPositionCloseReason.EXTERNAL_POSITION_CLOSED);
        position.markClosed(calculatePnl(position, closePrice), closePrice);
        managedPositionRepository.save(position);
        monitorService.pushPositionUpdate(position.getUserId(), position.getSymbol(), ManagedPositionResponseDto.from(position));
        log.warn("managed close rejected but Binance position is already closed positionId={}, symbol={}, errorCode={}, errorMessage={}, closeSkippedReason=POSITION_ALREADY_CLOSED",
                position.getId(), position.getSymbol(), binanceErrorCode(error), error.getMessage());
        return true;
    }

    private PositionRisk currentExchangePosition(ManagedPositionEntity position) {
        try {
            return clientService.getPositions().stream()
                    .filter(exchangePosition -> exchangePosition.getSymbol().equalsIgnoreCase(position.getSymbol()))
                    .findFirst()
                    .orElse(null);
        } catch (RuntimeException error) {
            log.warn("managed close actual position check failed positionId={}, symbol={}",
                    position.getId(), position.getSymbol(), error);
            throw error;
        }
    }

    private boolean sameDirection(ManagedPositionEntity position, PositionRisk actual) {
        if (actual == null || actual.getPositionAmt() == null) {
            return false;
        }
        return isLong(position) ? actual.getPositionAmt().signum() > 0 : actual.getPositionAmt().signum() < 0;
    }

    private String binanceErrorCode(RuntimeException error) {
        String message = error.getMessage();
        if (message == null) {
            return null;
        }
        int marker = message.indexOf("code=");
        if (marker < 0) {
            return null;
        }
        int start = marker + "code=".length();
        int end = message.indexOf(',', start);
        return end < 0 ? message.substring(start).trim() : message.substring(start, end).trim();
    }

    private String closeSkipReason(ManagedPositionEntity position) {
        if (position.getStatus() == ManagedPositionStatus.CLOSING || closingPositionIds.contains(position.getId())) {
            return "ALREADY_CLOSING";
        }
        if (position.getStatus() != ManagedPositionStatus.ACTIVE) {
            return "POSITION_NOT_ACTIVE";
        }
        if (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0) {
            return "INVALID_MARGIN";
        }
        if (position.getQuantity() == null || position.getQuantity().signum() <= 0) {
            return "INVALID_QUANTITY";
        }
        return "CONDITION_NOT_MATCHED";
    }

    private Map<String, String> closeCurrentPriceParams(ManagedPositionEntity position, BigDecimal closeQuantity) {
        ManagedOrderSymbolRule orderRule = orderRule(position.getSymbol());
        String quantity = adjustCloseQuantity(position.getSymbol(), closeQuantity, orderRule);
        String price = adjustClosePrice(position.getSymbol(), position.getCurrentPrice(), orderRule);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", position.getSymbol());
        params.put("side", position.getCloseSide());
        params.put("type", "LIMIT");
        params.put("timeInForce", "GTC");
        params.put("quantity", quantity);
        params.put("price", price);
        params.put("reduceOnly", "true");
        params.put("timestamp", String.valueOf(System.currentTimeMillis()));
        return params;
    }

    private ManagedOrderSymbolRule orderRule(String symbol) {
        String normalizedSymbol = requireText(symbol, "관리형 청산 심볼은 필수입니다.").toUpperCase(Locale.ROOT);
        return orderRuleCache.computeIfAbsent(normalizedSymbol, this::loadOrderRule);
    }

    private ManagedOrderSymbolRule loadOrderRule(String symbol) {
        return clientService.getExchangeSymbols().stream()
                .filter(exchangeSymbol -> exchangeSymbol.getSymbol().equalsIgnoreCase(symbol))
                .findFirst()
                .map(exchangeSymbol -> new ManagedOrderSymbolRule(
                        exchangeSymbol.getQuantityStepSize(),
                        exchangeSymbol.getMinQuantity(),
                        exchangeSymbol.getPriceTickSize()
                ))
                .orElse(ManagedOrderSymbolRule.empty());
    }

    private String adjustCloseQuantity(String symbol, BigDecimal quantity, ManagedOrderSymbolRule orderRule) {
        BigDecimal value = valueOrZero(quantity);
        BigDecimal adjusted = floorToStep(value, orderRule.quantityStepSize());
        if (orderRule.minQuantity() != null && adjusted.compareTo(orderRule.minQuantity()) < 0) {
            throw new IllegalArgumentException("관리형 청산 수량이 최소 수량보다 작습니다. symbol=" + symbol
                    + ", quantity=" + adjusted.toPlainString()
                    + ", minQuantity=" + orderRule.minQuantity().toPlainString());
        }
        if (adjusted.compareTo(value) != 0) {
            log.info("managed close quantity adjusted symbol={}, original={}, adjusted={}, stepSize={}",
                    symbol, value.toPlainString(), adjusted.toPlainString(), orderRule.quantityStepSize());
        }
        return plain(adjusted);
    }

    private String adjustClosePrice(String symbol, BigDecimal price, ManagedOrderSymbolRule orderRule) {
        BigDecimal value = valueOrZero(price);
        BigDecimal adjusted = floorToStep(value, orderRule.priceTickSize());
        if (adjusted.compareTo(value) != 0) {
            log.info("managed close price adjusted symbol={}, original={}, adjusted={}, tickSize={}",
                    symbol, value.toPlainString(), adjusted.toPlainString(), orderRule.priceTickSize());
        }
        return plain(adjusted);
    }

    private BigDecimal floorToStep(BigDecimal value, BigDecimal step) {
        if (step == null || step.signum() <= 0) {
            return value;
        }
        return value.divide(step, 0, RoundingMode.DOWN).multiply(step);
    }

    private String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    // 수수료와 펀딩비는 초기 구현에서 제외한다.
    private BigDecimal calculatePnl(ManagedPositionEntity position, BigDecimal price) {
        return isLong(position)
                ? price.subtract(position.getEntryPrice()).multiply(position.getQuantity())
                : position.getEntryPrice().subtract(price).multiply(position.getQuantity());
    }

    private void changeLeverage(String symbol, BigDecimal leverage) {
        BigDecimal normalized = leverage == null ? BigDecimal.ONE : leverage.stripTrailingZeros();
        if (normalized.scale() > 0 || normalized.compareTo(BigDecimal.ONE) < 0
                || normalized.compareTo(new BigDecimal("125")) > 0) {
            throw new IllegalArgumentException("레버리지는 1부터 125 사이의 정수여야 합니다.");
        }
        exchangeConnector.changeLeverage(Map.of(
                "symbol", requireText(symbol, "레버리지 변경 심볼은 필수입니다.").toUpperCase(Locale.ROOT),
                "leverage", normalized.toPlainString(),
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
        log.info("managed order leverage changed symbol={}, leverage={}", symbol, normalized);
    }

    private BigDecimal pnlPercent(ManagedPositionEntity position, BigDecimal pnl) {
        if (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return pnl.divide(position.getRequiredMargin(), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal pricePercent(ManagedPositionEntity position, BigDecimal price) {
        if (position.getEntryPrice() == null || position.getEntryPrice().signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal move = isLong(position)
                ? price.subtract(position.getEntryPrice())
                : position.getEntryPrice().subtract(price);
        return move.divide(position.getEntryPrice(), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal fillPrice(Order order, BigDecimal fallback) {
        if (order.getAvgPrice() != null && order.getAvgPrice().signum() > 0) return order.getAvgPrice();
        if (order.getPrice() != null && order.getPrice().signum() > 0) return order.getPrice();
        return fallback;
    }

    private void validateCreateRequest(ManagedAtrOrderRequestDto request) {
        if (!"LIMIT".equalsIgnoreCase(valueOrDefault(request.getType(), "LIMIT"))) {
            throw new IllegalArgumentException("관리형 ATR 주문은 LIMIT만 지원합니다.");
        }
        if (request.getEntryPrice() == null || request.getEntryPrice().signum() <= 0) {
            throw new IllegalArgumentException("관리형 ATR 주문은 entryPrice가 필요합니다.");
        }
        if (request.getMode() == ManagedOrderMode.RAISING_STOP_ONLY
                && request.isRaiseStopEnabled()
                && (request.getRaiseStopType() == null || request.getRaiseStopValue() == null)) {
            throw new IllegalArgumentException("손절선 상승 옵션은 raiseStopType, raiseStopValue가 필요합니다.");
        }
    }

    private String normalizeSide(String side) {
        String normalized = valueOrDefault(side, "BUY").toUpperCase(Locale.ROOT);
        if (!"BUY".equals(normalized) && !"SELL".equals(normalized)) {
            throw new IllegalArgumentException("side는 BUY 또는 SELL만 지원합니다.");
        }
        return normalized;
    }

    private boolean isLong(ManagedPositionEntity position) {
        return "BUY".equalsIgnoreCase(position.getEntrySide());
    }

    private BigDecimal max(BigDecimal a, BigDecimal b) {
        return a == null || b.compareTo(a) > 0 ? b : a;
    }

    private BigDecimal min(BigDecimal a, BigDecimal b) {
        return a == null || b.compareTo(a) < 0 ? b : a;
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal positiveOrNull(BigDecimal value, String fieldName) {
        if (value == null) {
            return null;
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + "는 0보다 커야 합니다.");
        }
        return value;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private record ExitPlan(BigDecimal stopPrice, BigDecimal targetPrice,
                            BigDecimal possibleLoss, BigDecimal possibleProfit,
                            TriggerBasis stopTriggerBasis, TriggerBasis takeProfitTriggerBasis) {
    }

    private record ManagedOrderSymbolRule(BigDecimal quantityStepSize, BigDecimal minQuantity, BigDecimal priceTickSize) {
        static ManagedOrderSymbolRule empty() {
            return new ManagedOrderSymbolRule(null, null, null);
        }
    }
}
