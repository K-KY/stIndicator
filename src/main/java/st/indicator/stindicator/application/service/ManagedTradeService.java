package st.indicator.stindicator.application.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import st.indicator.stindicator.application.dto.AtrOrderCommand;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.domain.entity.*;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionEntity;
import st.indicator.stindicator.infra.connector.entity.ManagedPositionJournalEntity;
import st.indicator.stindicator.infra.connector.entity.PendingOrderEntity;
import st.indicator.stindicator.infra.connector.repository.ManagedPositionJpaRepository;
import st.indicator.stindicator.infra.connector.repository.ManagedPositionJournalJpaRepository;
import st.indicator.stindicator.infra.connector.repository.PendingOrderJpaRepository;
import st.indicator.stindicator.infra.ws.MultiPlexManager;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalRequestDto;
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
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ManagedTradeService {
    private static final Logger log = LoggerFactory.getLogger(ManagedTradeService.class);
    private final ClientService clientService;
    private final ExchangeConnector exchangeConnector;
    private final PendingOrderJpaRepository pendingOrderRepository;
    private final ManagedPositionJpaRepository managedPositionRepository;
    private final ManagedPositionJournalJpaRepository managedPositionJournalRepository;
    private final MultiPlexManager multiPlexManager;
    private final Set<Long> closingPositionIds = ConcurrentHashMap.newKeySet();
    private final Map<String, ManagedOrderSymbolRule> orderRuleCache = new ConcurrentHashMap<>();

    public ManagedTradeService(ClientService clientService,
                               ExchangeConnector exchangeConnector,
                               PendingOrderJpaRepository pendingOrderRepository,
                               ManagedPositionJpaRepository managedPositionRepository,
                               ManagedPositionJournalJpaRepository managedPositionJournalRepository,
                               MultiPlexManager multiPlexManager) {
        this.clientService = clientService;
        this.exchangeConnector = exchangeConnector;
        this.pendingOrderRepository = pendingOrderRepository;
        this.managedPositionRepository = managedPositionRepository;
        this.managedPositionJournalRepository = managedPositionJournalRepository;
        this.multiPlexManager = multiPlexManager;
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

    private void migrateLegacyPositionIfNeeded(ManagedPositionEntity position) {
        if (!position.isLegacyManagedPolicy()) {
            return;
        }
        log.warn("legacy managed position detected id={}, symbol={}, mode={}, entryPrice={}, initialStop={}, currentStop={}, target={}",
                position.getId(), position.getSymbol(), position.getMode(), position.getEntryPrice(),
                position.getInitialStopPrice(), position.getCurrentStopPrice(), position.getTargetPrice());
        position.applyLegacyFixedPolicy();
        managedPositionRepository.save(position);
        log.warn("legacy managed position migrated id={}, symbol={}, mode={}, raiseStopEnabled={}, initialStop={}, currentStop={}, target={}",
                position.getId(), position.getSymbol(), position.getMode(), position.isRaiseStopEnabled(),
                position.getInitialStopPrice(), position.getCurrentStopPrice(), position.getTargetPrice());
    }

    @Transactional
    public PendingOrderEntity createAtrLimitOrder(ManagedAtrOrderRequestDto request) {
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
        Order order = clientService.order(new st.indicator.stindicator.application.dto.OrderCommand(
                preview.getSymbol(),
                normalizeSide(preview.getSide()),
                "LIMIT",
                valueOrDefault(request.getTimeInForce(), "GTC"),
                preview.getQuantity().toPlainString(),
                preview.getEntryPrice().toPlainString()
        ));
        PendingOrderEntity pending = PendingOrderEntity.create(
                null,
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
                request.getRaiseStopValue()
        );
        normalizePendingPolicyIfNeeded(pending);
        PendingOrderEntity saved = pendingOrderRepository.save(pending);
        multiPlexManager.subscribeMarkPrice(saved.getSymbol());
        log.info("managed pending order created id={}, symbol={}, orderId={}, quantity={}, requiredMargin={}",
                saved.getId(), saved.getSymbol(), saved.getOrderId(), saved.getQuantity(), saved.getRequiredMargin());
        return saved;
    }

    @Transactional
    public List<PendingOrderEntity> pendingOrders() {
        List<PendingOrderEntity> pendingOrders = pendingOrderRepository.findAllByStatusOrderByCreatedAtDesc(PendingOrderStatus.PENDING);
        pendingOrders.forEach(this::repairPendingSizingIfNeeded);
        pendingOrders.forEach(this::normalizePendingPolicyIfNeeded);
        return pendingOrders;
    }

    @Transactional
    public PendingOrderEntity pendingOrder(Long id) {
        PendingOrderEntity pending = pendingOrderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("대기 주문을 찾을 수 없습니다: " + id));
        pending = repairPendingSizingIfNeeded(pending);
        normalizePendingPolicyIfNeeded(pending);
        return pending;
    }

    @Transactional
    public PendingOrderEntity cancelPendingOrder(Long id) {
        PendingOrderEntity pending = pendingOrder(id);
        if (pending.getStatus() != PendingOrderStatus.PENDING) {
            return pending;
        }
        clientService.cancelOrder(pending.getSymbol(), pending.getOrderId());
        pending.mark(PendingOrderStatus.CANCELED);
        return pendingOrderRepository.save(pending);
    }

    @Transactional
    public PendingOrderEntity updatePendingConditions(Long id, UpdatePendingOrderConditionsRequestDto request) {
        PendingOrderEntity pending = pendingOrder(id);
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

    public List<ManagedPositionEntity> activePositions() {
        return managedPositionRepository.findAllByStatusOrderByOpenedAtDesc(ManagedPositionStatus.ACTIVE);
    }

    public List<ManagedPositionEntity> positionHistory(String symbol, String side, ManagedOrderMode mode,
                                                       ManagedPositionCloseReason closeReason) {
        return managedPositionRepository.findAllByStatusInOrderByClosedAtDesc(
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

    @Transactional(readOnly = true)
    public List<ManagedPositionJournalEntity> journals(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return managedPositionJournalRepository.findAllByOrderByUpdatedAtDesc();
        }
        return managedPositionJournalRepository.findAllByManagedPosition_SymbolIgnoreCaseOrderByUpdatedAtDesc(symbol);
    }

    @Transactional(readOnly = true)
    public ManagedPositionJournalEntity journal(Long positionId) {
        return managedPositionJournalRepository.findByManagedPosition_Id(positionId)
                .orElseThrow(() -> new IllegalArgumentException("매매일지를 찾을 수 없습니다: " + positionId));
    }

    @Transactional
    public ManagedPositionJournalEntity upsertJournal(Long positionId, Long userId,
                                                      ManagedPositionJournalRequestDto request) {
        ManagedPositionEntity position = position(positionId);
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
    public void deleteJournal(Long positionId) {
        managedPositionJournalRepository.findByManagedPosition_Id(positionId)
                .ifPresent(managedPositionJournalRepository::delete);
    }

    @Transactional
    public ManagedPositionEntity closePosition(Long id) {
        ManagedPositionEntity position = position(id);
        closePosition(position, ManagedPositionCloseReason.MANUAL_CLOSE);
        return position(position.getId());
    }

    @EventListener
    @Transactional
    public void handleOrderTradeUpdate(OrderTradeUpdateEvent event) {
        if (event == null || event.orderId() == null) {
            return;
        }
        pendingOrderRepository.findFirstByOrderId(event.orderId())
                .ifPresent(pending -> handlePendingOrderUpdate(pending, event));
        managedPositionRepository.findFirstByCloseOrderId(event.orderId())
                .ifPresent(position -> handleCloseOrderUpdate(position, event));
    }

    @EventListener
    @Transactional
    public void handlePriceTick(PriceTickEvent event) {
        if (event == null || event.price() == null) {
            return;
        }
        managedPositionRepository.findAllBySymbolAndStatus(event.symbol(), ManagedPositionStatus.ACTIVE)
                .forEach(position -> evaluatePosition(position, event.price()));
    }

    private void handlePendingOrderUpdate(PendingOrderEntity pending, OrderTradeUpdateEvent event) {
        if (pending.getStatus() != PendingOrderStatus.PENDING) {
            return;
        }
        if (event.isFilled()) {
            pending = repairPendingSizingIfNeeded(pending);
            pending.mark(PendingOrderStatus.FILLED);
            pendingOrderRepository.save(pending);
            ManagedPositionEntity position = managedPositionRepository.save(
                    ManagedPositionEntity.from(pending, event.averagePrice())
            );
            multiPlexManager.subscribeMarkPrice(position.getSymbol());
            log.info("managed position opened id={}, symbol={}, entryOrderId={}",
                    position.getId(), position.getSymbol(), position.getEntryOrderId());
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
        log.info("managed position closed id={}, reason={}, realizedPnl={}",
                position.getId(), position.getCloseReason(), realized);
    }

    private void evaluatePosition(ManagedPositionEntity position, BigDecimal currentPrice) {
        if (position.isLegacyManagedPolicy()) {
            log.warn("legacy managed position found during price event id={}, symbol={}, mode={}, initialStop={}, currentStop={}, target={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), position.getTargetPrice());
            migrateLegacyPositionIfNeeded(position);
            position = position(position.getId());
        }
        BigDecimal pnl = calculatePnl(position, currentPrice);
        BigDecimal highest = max(position.getHighestPrice(), currentPrice);
        BigDecimal lowest = min(position.getLowestPrice(), currentPrice);
        BigDecimal nextStop = position.getCurrentStopPrice();
        boolean raiseActivated = position.isRaiseActivated();

        ManagedPositionCloseReason reason = fixedCloseReason(position, currentPrice, pnl);
        if (position.getMode() == ManagedOrderMode.RAISING_STOP_ONLY) {
            log.debug("raising stop inspect id={}, symbol={}, mode={}, initialStop={}, currentStop={}, price={}, pnl={}, raiseActivated={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), currentPrice, pnl, raiseActivated);
            if (!raiseActivated && shouldActivateRaisingStop(position, currentPrice, pnl)) {
                raiseActivated = true;
            }
            if (raiseActivated && position.isRaiseStopEnabled()) {
                nextStop = nextRaisedStop(position, currentPrice, pnl);
            }
            reason = raisingCloseReason(position, currentPrice, pnl, nextStop, raiseActivated);
        }
        if (position.getMode() != ManagedOrderMode.RAISING_STOP_ONLY && nextStop.compareTo(position.getCurrentStopPrice()) != 0) {
            log.warn("fixed position stop mutation blocked id={}, symbol={}, mode={}, initialStop={}, currentStop={}, attemptedStop={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), nextStop);
            nextStop = position.getCurrentStopPrice();
            raiseActivated = false;
        }
        if (nextStop.compareTo(position.getCurrentStopPrice()) != 0) {
            log.info("managed stop changed id={}, symbol={}, mode={}, initialStop={}, previousStop={}, nextStop={}, price={}, pnl={}",
                    position.getId(), position.getSymbol(), position.getMode(), position.getInitialStopPrice(),
                    position.getCurrentStopPrice(), nextStop, currentPrice, pnl);
            position.appendEvent("STOP_CHANGED previousStop=" + position.getCurrentStopPrice()
                    + ", nextStop=" + nextStop + ", price=" + currentPrice + ", pnl=" + pnl);
        }

        position.updateMarket(currentPrice, pnl, highest, lowest, nextStop, raiseActivated);
        managedPositionRepository.save(position);
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

    private boolean shouldActivateRaisingStop(ManagedPositionEntity position, BigDecimal currentPrice, BigDecimal currentPnl) {
        BigDecimal triggerValue = valueOrZero(position.getRaiseTriggerValue());
        if (triggerValue.signum() <= 0) {
            return currentPnl.signum() > 0;
        }
        RaiseStopType triggerType = position.getRaiseTriggerType() == null ? RaiseStopType.PERCENT : position.getRaiseTriggerType();
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            if (triggerType == RaiseStopType.AMOUNT) {
                return currentPnl.compareTo(triggerValue) >= 0;
            }
            if (position.getRequiredMargin() == null || position.getRequiredMargin().signum() <= 0) {
                return false;
            }
            BigDecimal pnlPercent = currentPnl
                    .divide(position.getRequiredMargin(), 10, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
            return pnlPercent.compareTo(triggerValue) >= 0;
        }

        BigDecimal favorableMove = isLong(position)
                ? currentPrice.subtract(position.getEntryPrice())
                : position.getEntryPrice().subtract(currentPrice);
        if (favorableMove.signum() <= 0) {
            return false;
        }
        if (triggerType == RaiseStopType.AMOUNT) {
            return favorableMove.compareTo(triggerValue) >= 0;
        }
        BigDecimal priceMovePercent = favorableMove
                .divide(position.getEntryPrice(), 10, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        return priceMovePercent.compareTo(triggerValue) >= 0;
    }

    private ManagedPositionCloseReason fixedCloseReason(ManagedPositionEntity position, BigDecimal price, BigDecimal pnl) {
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            if (position.getPossibleLoss() != null && pnl.compareTo(position.getPossibleLoss().negate()) <= 0) {
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
            BigDecimal stopPnlLine = activated
                    ? calculatePnl(position, stop)
                    : position.getPossibleLoss().negate();
            if (pnl.compareTo(stopPnlLine) <= 0) {
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
        if (position.getStatus() != ManagedPositionStatus.ACTIVE || !closingPositionIds.add(position.getId())) {
            return;
        }
        position.markClosing(null, reason);
        managedPositionRepository.save(position);
        try {
            Order order = exchangeConnector.order(closeCurrentPriceParams(position));
            position.markClosing(order.getOrderId(), reason);
            if ("FILLED".equalsIgnoreCase(order.getStatus())) {
                BigDecimal closePrice = fillPrice(order, position.getCurrentPrice());
                position.markClosed(calculatePnl(position, closePrice), closePrice);
                closingPositionIds.remove(position.getId());
            }
            managedPositionRepository.save(position);
        } catch (RuntimeException e) {
            position.markFailed();
            closingPositionIds.remove(position.getId());
            managedPositionRepository.save(position);
            throw e;
        }
    }

    private Map<String, String> closeCurrentPriceParams(ManagedPositionEntity position) {
        ManagedOrderSymbolRule orderRule = orderRule(position.getSymbol());
        String quantity = adjustCloseQuantity(position.getSymbol(), position.getQuantity(), orderRule);
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

    private BigDecimal nextRaisedStop(ManagedPositionEntity position, BigDecimal currentPrice, BigDecimal currentPnl) {
        BigDecimal currentStop = position.getCurrentStopPrice();
        if (position.getStopTriggerBasis() == TriggerBasis.PNL_PERCENT) {
            BigDecimal protectedPnl;
            if (position.getRaiseStopType() == RaiseStopType.AMOUNT) {
                protectedPnl = valueOrZero(position.getRaiseStopValue());
            } else {
                BigDecimal percent = valueOrZero(position.getRaiseStopValue()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
                protectedPnl = currentPnl.multiply(percent);
            }
            BigDecimal candidate = priceAtPnl(position, protectedPnl);
            if (isLong(position)) {
                return candidate.compareTo(currentStop) > 0 ? candidate : currentStop;
            }
            return candidate.compareTo(currentStop) < 0 ? candidate : currentStop;
        }

        BigDecimal candidate;
        if (position.getRaiseStopType() == RaiseStopType.AMOUNT) {
            candidate = isLong(position)
                    ? currentPrice.subtract(valueOrZero(position.getRaiseStopValue()))
                    : currentPrice.add(valueOrZero(position.getRaiseStopValue()));
        } else {
            BigDecimal percent = valueOrZero(position.getRaiseStopValue()).divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP);
            BigDecimal profitGap = isLong(position)
                    ? currentPrice.subtract(position.getEntryPrice())
                    : position.getEntryPrice().subtract(currentPrice);
            BigDecimal protectedProfit = profitGap.multiply(percent);
            candidate = isLong(position)
                    ? position.getEntryPrice().add(protectedProfit)
                    : position.getEntryPrice().subtract(protectedProfit);
        }
        if (isLong(position)) {
            return candidate.compareTo(currentStop) > 0 ? candidate : currentStop;
        }
        return candidate.compareTo(currentStop) < 0 ? candidate : currentStop;
    }

    // 수수료와 펀딩비는 초기 구현에서 제외한다.
    private BigDecimal calculatePnl(ManagedPositionEntity position, BigDecimal price) {
        return isLong(position)
                ? price.subtract(position.getEntryPrice()).multiply(position.getQuantity())
                : position.getEntryPrice().subtract(price).multiply(position.getQuantity());
    }

    private BigDecimal priceAtPnl(ManagedPositionEntity position, BigDecimal pnl) {
        if (position.getQuantity() == null || position.getQuantity().signum() == 0) {
            return position.getEntryPrice();
        }
        BigDecimal priceMove = pnl.divide(position.getQuantity(), 10, RoundingMode.HALF_UP);
        return isLong(position)
                ? position.getEntryPrice().add(priceMove)
                : position.getEntryPrice().subtract(priceMove);
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
