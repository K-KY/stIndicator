package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.AtrOrderCommand;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.application.dto.CandleCommand;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.application.exception.BalanceFetchFailException;
import st.indicator.stindicator.application.exception.CandleFetchFailException;
import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BinanceClient implements ClientService {
    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);
    private final ExchangeConnector exchangeConnector;
    private final AtrPositionSizingService atrPositionSizingService;
    private final Map<String, OrderSymbolRule> orderRuleCache = new ConcurrentHashMap<>();

    public BinanceClient(ExchangeConnector exchangeConnector, AtrPositionSizingService atrPositionSizingService) {
        this.exchangeConnector = exchangeConnector;
        this.atrPositionSizingService = atrPositionSizingService;
    }

    @Override
    public BigDecimal getBalance() {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("flow getBalance start timestamp={}", currentTimeMillis);
        try {
            BigDecimal balance = exchangeConnector.getBalance(Map.of("timestamp", String.valueOf(currentTimeMillis)));
            log.info("flow getBalance done balance={}", balance);
            return balance;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new BalanceFetchFailException(e, "지갑 조회 실패");
        }
    }

    //symbol의 차트에서 interval간격 봉으로 limit만큼 데이터 가져옴
    @Override
    public List<Candle> getCandles(CandleCommand dto) {
        log.info("flow getCandles start symbol={}, interval={}, limit={}",
                dto.getSymbol(), dto.getInterval(), dto.getLimit());
        try {
            Map<String, String> params = new LinkedHashMap<>();
            params.put("symbol", valueOrDefault(dto.getSymbol(), "BTCUSDT").toUpperCase(Locale.ROOT));
            params.put("interval", valueOrDefault(dto.getInterval(), "4h"));
            params.put("limit", valueOrDefault(dto.getLimit(), "150"));
            List<Candle> candles = exchangeConnector.getCandles(
                    params
            );
            log.info("flow getCandles done symbol={}, count={}", dto.getSymbol(), candles.size());
            return candles;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new CandleFetchFailException(e, "캔들 조회 실패");
        }
    }

    @Override
    public BigDecimal getAtr(CandleCommand dto) {
        BigDecimal atr = atrPositionSizingService.calculateAtr(getCandles(dto), 14);
        log.info("flow getAtr done symbol={}, interval={}, limit={}, atr={}",
                dto.getSymbol(), dto.getInterval(), dto.getLimit(), atr);
        return atr;
    }

    @Override
    public Order order(OrderCommand dto) {
        log.info("flow order start symbol={}, side={}, type={}, timeInForce={}, quantity={}, price={}",
                dto.getSymbol(), dto.getSide(), dto.getType(), dto.getTimeInForce(), dto.getQuantity(), dto.getPrice());
        String symbol = requireText(dto.getSymbol(), "주문 심볼은 필수입니다.");
        return placeOrder(symbol, dto.getSide(), dto.getType(), dto.getTimeInForce(),
                dto.getQuantity(), dto.getPrice(), false);
    }

    @Override
    public List<AssetBalance> getAssets() {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("flow getAssets start timestamp={}", currentTimeMillis);
        try {
            List<AssetBalance> assets = exchangeConnector.getAssets(Map.of("timestamp", String.valueOf(currentTimeMillis)));
            log.info("flow getAssets done count={}, taken={}ms", assets.size(), System.currentTimeMillis() - currentTimeMillis);
            return assets;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new BalanceFetchFailException(e, "자산 목록 조회 실패");
        }
    }

    @Override
    public List<ExchangeSymbol> getExchangeSymbols() {
        log.info("flow getExchangeSymbols start");
        try {
            List<ExchangeSymbol> symbols = exchangeConnector.getExchangeSymbols();
            log.info("flow getExchangeSymbols done count={}", symbols.size());
            return symbols;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("거래소 심볼 목록 조회 실패", e);
        }
    }

    @Override
    public SymbolPrice getPrice(String symbol) {
        log.info("flow getPrice start symbol={}", symbol);
        try {
            SymbolPrice symbolPrice = exchangeConnector.getPrice(Map.of("symbol", symbol));
            log.info("flow getPrice done symbol={}, price={}", symbolPrice.getSymbol(), symbolPrice.getPrice());
            return symbolPrice;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("현재가 조회 실패", e);
        }
    }

    @Override
    public AtrOrderPreview previewAtrOrder(AtrOrderCommand dto) {
        log.info("flow previewAtrOrder start symbol={}, side={}, interval={}, limit={}, atrPeriod={}, riskPercent={}, atrMultiplier={}, leverage={}, entryPrice={}",
                dto.getSymbol(), dto.getSide(), dto.getInterval(), dto.getLimit(), dto.getAtrPeriod(),
                dto.getRiskPercent(), dto.getAtrMultiplier(), dto.getLeverage(), dto.getEntryPrice());
        BigDecimal availableBalance = getAvailableBalance();
        SymbolPrice currentPrice = getPrice(dto.getSymbol());
        BigDecimal entryPrice = dto.getEntryPrice() != null ? dto.getEntryPrice() : currentPrice.getPrice();
        int atrPeriod = dto.getAtrPeriod() == null ? 14 : dto.getAtrPeriod();

        BigDecimal atr = atrPositionSizingService.calculateAtr(
                getCandles(new CandleCommand(dto.getSymbol(), dto.getInterval(), dto.getLimit())),
                atrPeriod
        );

        AtrOrderPreview preview = atrPositionSizingService.preview(
                dto.getSymbol(),
                dto.getSide(),
                dto.getInterval(),
                atrPeriod,
                availableBalance,
                entryPrice,
                atr,
                dto.getRiskPercent(),
                dto.getAtrMultiplier(),
                dto.getLeverage(),
                dto.getStopTriggerBasis(),
                dto.getTakeProfitTriggerBasis()
        );
        log.info("flow previewAtrOrder done symbol={}, atr={}, quantity={}, requiredMargin={}, stopPrice={}, targetPrice={}",
                preview.getSymbol(), preview.getAtr(), preview.getQuantity(), preview.getRequiredMargin(),
                preview.getStopPrice(), preview.getTargetPrice());
        return preview;
    }

    @Override
    public Order orderByAtr(AtrOrderCommand dto) {
        log.info("flow orderByAtr start symbol={}, side={}, type={}, leverage={}",
                dto.getSymbol(), dto.getSide(), dto.getType(), dto.getLeverage());
        AtrOrderPreview preview = previewAtrOrder(dto);
        String type = resolveAtrOrderType(dto);
        String timeInForce = "LIMIT".equals(type)
                ? valueOrDefault(dto.getTimeInForce(), "GTC").toUpperCase(Locale.ROOT)
                : null;
        String price = "LIMIT".equals(type) ? preview.getEntryPrice().toPlainString() : null;
        Order order = placeOrder(
                dto.getSymbol(),
                dto.getSide(),
                type,
                timeInForce,
                preview.getQuantity().toPlainString(),
                price,
                false
        );
        log.info("flow orderByAtr done symbol={}, orderId={}, quantity={}",
                order.getSymbol(), order.getOrderId(), preview.getQuantity());
        return order;
    }

    @Override
    public void getOrders() {

    }

    @Override
    public Order getOrderDetail(String symbol, String orderId) {
        log.info("flow getOrderDetail start symbol={}, orderId={}", symbol, orderId);
        return exchangeConnector.orderDetail(Map.of(
                "symbol", requireText(symbol, "주문 상세 조회 심볼은 필수입니다."),
                "orderId", requireText(orderId, "주문 ID는 필수입니다."),
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
    }

    @Override
    public Order cancelOrder(String symbol, String orderId) {
        log.info("flow cancelOrder start symbol={}, orderId={}", symbol, orderId);
        Order order = exchangeConnector.cancelOrder(Map.of(
                "symbol", requireText(symbol, "주문 취소 심볼은 필수입니다."),
                "orderId", requireText(orderId, "주문 ID는 필수입니다."),
                "timestamp", String.valueOf(System.currentTimeMillis())
        ));
        log.info("flow cancelOrder done symbol={}, orderId={}, status={}", order.getSymbol(), order.getOrderId(), order.getStatus());
        return order;
    }


    @Override
    public List<PositionRisk> getPositions() {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("flow getPositions start timestamp={}", currentTimeMillis);
        try {
            List<PositionRisk> positions = exchangeConnector.getPositions(Map.of("timestamp", String.valueOf(currentTimeMillis)));
            log.info("flow getPositions done count={}, taken={}", positions.size(), System.currentTimeMillis() - currentTimeMillis);
            return positions;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("포지션 조회 실패", e);
        }
    }

    @Override
    public Order liquidatePosition(String symbol) {
        log.info("flow liquidatePosition start symbol={}", symbol);
        PositionRisk positionRisk = getPositions().stream()
                .filter(position -> position.getSymbol().equalsIgnoreCase(symbol))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("청산할 포지션이 없습니다: " + symbol));

        String side = positionRisk.getPositionAmt().signum() > 0 ? "SELL" : "BUY";
        Order order = placeOrder(
                positionRisk.getSymbol(),
                side,
                "MARKET",
                null,
                positionRisk.getPositionAmt().abs().toPlainString(),
                null,
                true
        );
        log.info("flow liquidatePosition done symbol={}, orderId={}, closeSide={}, quantity={}",
                order.getSymbol(), order.getOrderId(), side, positionRisk.getPositionAmt().abs());
        return order;
    }

    private BigDecimal getAvailableBalance() {
        long currentTimeMillis = System.currentTimeMillis();
        log.info("flow getAvailableBalance start timestamp={}", currentTimeMillis);
        try {
            BigDecimal availableBalance = exchangeConnector.getAvailableBalance(Map.of("timestamp", String.valueOf(currentTimeMillis)));
            log.info("flow getAvailableBalance done availableBalance={}, taken={}", availableBalance, System.currentTimeMillis() - currentTimeMillis);
            return availableBalance;
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new BalanceFetchFailException(e, "가용 자산 조회 실패");
        }
    }

    private Order placeOrder(String symbol, String side, String type, String timeInForce,
                             String quantity, String price, boolean reduceOnly) {
        long timeMillis = System.currentTimeMillis();
        log.info("flow placeOrder start symbol={}, side={}, type={}, timeInForce={}, quantity={}, price={}, reduceOnly={}",
                symbol, side, type, timeInForce, quantity, price, reduceOnly);
        String normalizedType = type == null ? "MARKET" : type.toUpperCase(Locale.ROOT);
        OrderSymbolRule orderRule = orderRule(symbol);
        String adjustedQuantity = adjustQuantity(symbol, quantity, orderRule);
        String adjustedPrice = "LIMIT".equals(normalizedType) ? adjustPrice(symbol, price, orderRule) : null;
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side == null ? "BUY" : side.toUpperCase(Locale.ROOT));
        params.put("type", normalizedType);
        params.put("quantity", adjustedQuantity);
        params.put("timestamp", String.valueOf(timeMillis));

        if ("LIMIT".equals(normalizedType) && timeInForce != null && !timeInForce.isBlank()) {
            params.put("timeInForce", timeInForce.toUpperCase(Locale.ROOT));
        }
        if ("LIMIT".equals(normalizedType) && adjustedPrice != null && !adjustedPrice.isBlank()) {
            params.put("price", adjustedPrice);
        }
        if (reduceOnly) {
            params.put("reduceOnly", "true");
        }

        Order order = normalizeOrder(exchangeConnector.order(params), symbol, side, params.get("type"),
                params.get("timeInForce"), adjustedQuantity, params.get("price"), reduceOnly);
        log.info("flow placeOrder done symbol={}, orderId={}, status={}, executedQty={}",
                order.getSymbol(), order.getOrderId(), order.getStatus(), order.getExecutedQty());
        return order;
    }

    private String resolveAtrOrderType(AtrOrderCommand dto) {
        if (dto.getEntryPrice() != null) {
            return "LIMIT";
        }
        return dto.getType() == null || dto.getType().isBlank()
                ? "MARKET"
                : dto.getType().toUpperCase(Locale.ROOT);
    }

    private Order normalizeOrder(Order order, String symbol, String side, String type, String timeInForce,
                                 String quantity, String price, boolean reduceOnly) {
        if (order == null) {
            return new Order(null, symbol, null, null, decimalOrZero(price), BigDecimal.ZERO,
                    decimalOrZero(quantity), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    timeInForce, type, reduceOnly, false, side, null, BigDecimal.ZERO,
                    null, false, type, null, null, null, null);
        }

        if (order.getOrigQty() != null && order.getPrice() != null
                && order.getSymbol() != null && order.getSide() != null && order.getType() != null) {
            return order;
        }

        return new Order(
                order.getOrderId(),
                valueOrDefault(order.getSymbol(), symbol),
                order.getStatus(),
                order.getClientOrderId(),
                order.getPrice() == null ? decimalOrZero(price) : order.getPrice(),
                order.getAvgPrice(),
                order.getOrigQty() == null ? decimalOrZero(quantity) : order.getOrigQty(),
                order.getExecutedQty(),
                order.getCumQty(),
                order.getCumQuote(),
                valueOrDefault(order.getTimeInForce(), timeInForce),
                valueOrDefault(order.getType(), type),
                order.getReduceOnly() == null ? reduceOnly : order.getReduceOnly(),
                order.getClosePosition(),
                valueOrDefault(order.getSide(), side),
                order.getPositionSide(),
                order.getStopPrice(),
                order.getWorkingType(),
                order.getPriceProtect(),
                valueOrDefault(order.getOrigType(), type),
                order.getPriceMatch(),
                order.getSelfTradePreventionMode(),
                order.getGoodTillDate(),
                order.getUpdateTime()
        );
    }

    private BigDecimal decimalOrZero(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value);
    }

    private OrderSymbolRule orderRule(String symbol) {
        String normalizedSymbol = requireText(symbol, "주문 심볼은 필수입니다.").toUpperCase(Locale.ROOT);
        return orderRuleCache.computeIfAbsent(normalizedSymbol, this::loadOrderRule);
    }

    private OrderSymbolRule loadOrderRule(String symbol) {
        try {
            return exchangeConnector.getExchangeSymbols().stream()
                    .filter(exchangeSymbol -> exchangeSymbol.getSymbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .map(exchangeSymbol -> new OrderSymbolRule(
                            exchangeSymbol.getQuantityStepSize(),
                            exchangeSymbol.getMinQuantity(),
                            exchangeSymbol.getPriceTickSize()
                    ))
                    .orElse(OrderSymbolRule.empty());
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("주문 심볼 필터 조회 실패: " + symbol, e);
        }
    }

    private String adjustQuantity(String symbol, String quantity, OrderSymbolRule orderRule) {
        BigDecimal value = decimalOrZero(quantity);
        BigDecimal adjusted = floorToStep(value, orderRule.quantityStepSize());
        if (orderRule.minQuantity() != null && adjusted.compareTo(orderRule.minQuantity()) < 0) {
            throw new IllegalArgumentException("주문 수량이 최소 수량보다 작습니다. symbol=" + symbol
                    + ", quantity=" + adjusted.toPlainString()
                    + ", minQuantity=" + orderRule.minQuantity().toPlainString());
        }
        if (adjusted.compareTo(value) != 0) {
            log.info("flow adjustOrderQuantity symbol={}, original={}, adjusted={}, stepSize={}",
                    symbol, value.toPlainString(), adjusted.toPlainString(), orderRule.quantityStepSize());
        }
        return plain(adjusted);
    }

    private String adjustPrice(String symbol, String price, OrderSymbolRule orderRule) {
        if (price == null || price.isBlank()) {
            return null;
        }
        BigDecimal value = decimalOrZero(price);
        BigDecimal adjusted = floorToStep(value, orderRule.priceTickSize());
        if (adjusted.compareTo(value) != 0) {
            log.info("flow adjustOrderPrice symbol={}, original={}, adjusted={}, tickSize={}",
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

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private record OrderSymbolRule(BigDecimal quantityStepSize, BigDecimal minQuantity, BigDecimal priceTickSize) {
        static OrderSymbolRule empty() {
            return new OrderSymbolRule(null, null, null);
        }
    }
}
