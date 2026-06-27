package st.indicator.stindicator.infra.connector.exchange;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.indicator.stindicator.application.service.ExchangeConnector;
import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;
import st.indicator.stindicator.domain.utils.candle.Candle;
import st.indicator.stindicator.domain.utils.candle.CandleMapper;
import st.indicator.stindicator.domain.utils.client.ExchangeClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Component
public class BinanceConnector implements ExchangeConnector {
    private static final Logger log = LoggerFactory.getLogger(BinanceConnector.class);
    private static final String ACCOUNT_PATH = "https://fapi.binance.com/fapi/v3/account";
    private static final String TOTAL_WALLET_BALANCE = "totalWalletBalance";
    private static final String CANDLE_PATH = "https://fapi.binance.com/fapi/v1/klines";
    private static final String ORDER_PATH = "https://fapi.binance.com/fapi/v1/order";
    private static final String LEVERAGE_PATH = "https://fapi.binance.com/fapi/v1/leverage";
    private static final String POSITION_RISK_PATH = "https://fapi.binance.com/fapi/v3/positionRisk";
    private static final String POSITION_RISK_V2_PATH = "https://fapi.binance.com/fapi/v2/positionRisk";
    private static final String EXCHANGE_INFO_PATH = "https://fapi.binance.com/fapi/v1/exchangeInfo";
    private static final String TICKER_24HR_PATH = "https://fapi.binance.com/fapi/v1/ticker/24hr";
    private static final String PRICE_PATH = "https://fapi.binance.com/fapi/v1/ticker/price";
    private static final String AVAILABLE_BALANCE = "availableBalance";
    private static final CandleMapper candleMapper = new CandleMapper();
    private final ExchangeClient exchangeClient;
    private final ObjectMapper objectMapper;

    public BinanceConnector(ExchangeClient exchangeClient, ObjectMapper objectMapper) {
        this.exchangeClient = exchangeClient;
        this.objectMapper = objectMapper;
    }

    public List<Candle> getCandles(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String s = exchangeClient.get(CANDLE_PATH, params);
        assertBinanceSuccess(s, "candles");
        return candleMapper.map(s);
    }

    public BigDecimal getBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String s = exchangeClient.get(ACCOUNT_PATH, params);
        JsonNode account = readSuccessTree(s, "account balance");
        return requiredDecimal(account, TOTAL_WALLET_BALANCE, "account balance");
    }

    @Override
    public BigDecimal getAvailableBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String s = exchangeClient.get(ACCOUNT_PATH, params);
        JsonNode account = readSuccessTree(s, "available balance");
        return requiredDecimal(account, AVAILABLE_BALANCE, "available balance");
    }

    @Override
    public List<AssetBalance> getAssets(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(ACCOUNT_PATH, params);
        JsonNode root = readSuccessTree(response, "account assets");
        JsonNode assetsNode = requiredNode(root, "assets", "account assets");
        List<AssetBalance> assets = new ArrayList<>();
        for (JsonNode assetNode : assetsNode) {
            BigDecimal walletBalance = decimal(assetNode, "walletBalance");
            BigDecimal availableBalance = decimal(assetNode, "availableBalance");
            BigDecimal unrealizedProfit = decimal(assetNode, "unrealizedProfit");
            // 불필요한 자산 정보를 필터링
            // 현재 사용중이지 않은 옵션에 대한 정보
            if (walletBalance.signum() == 0 && availableBalance.signum() == 0 && unrealizedProfit.signum() == 0) {
                continue;
            }
            assets.add(new AssetBalance(
                    assetNode.get("asset").asString(),
                    walletBalance,
                    availableBalance,
                    unrealizedProfit
            ));
        }
        return assets;
    }

    @Override
    public List<PositionRisk> getPositions(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        try {
            List<PositionRisk> positions = getPositions(POSITION_RISK_PATH, params, "position risk v3");
            log.info("Binance position risk v3 parsed activeCount={}", positions.size());
            return positions;
        } catch (RuntimeException | IOException error) {
            log.warn("Binance position risk v3 failed, fallback to v2. reason={}", error.getMessage());
            List<PositionRisk> positions = getPositions(POSITION_RISK_V2_PATH, params, "position risk v2");
            log.info("Binance position risk v2 parsed activeCount={}", positions.size());
            return positions;
        }
    }

    private List<PositionRisk> getPositions(String path, Map<String, String> params, String operation)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(path, params);
        JsonNode root = readSuccessTree(response, operation);
        JsonNode positionsNode = positionsNode(root, operation);
        List<PositionRisk> positions = new ArrayList<>();
        for (JsonNode positionNode : positionsNode) {
            BigDecimal positionAmt = decimal(positionNode, "positionAmt");
            if (positionAmt.signum() == 0) {
                continue;
            }
            positions.add(new PositionRisk(
                    requiredText(positionNode, "symbol", operation),
                    positionAmt,
                    decimal(positionNode, "entryPrice"),
                    decimal(positionNode, "markPrice"),
                    decimalAny(positionNode, "unRealizedProfit", "unrealizedProfit"),
                    decimal(positionNode, "leverage"),
                    decimal(positionNode, "liquidationPrice"),
                    decimalAny(positionNode, "notional", "notionalValue"),
                    decimalAny(positionNode, "positionInitialMargin", "initialMargin", "isolatedWallet"),
                    textOrDefault(positionNode, "positionSide", "BOTH")
            ));
        }
        return positions;
    }

    private JsonNode positionsNode(JsonNode root, String operation) {
        if (root == null) {
            throw new IllegalStateException("Binance " + operation + " response is empty");
        }
        if (root.isArray()) {
            return root;
        }
        JsonNode positions = root.get("positions");
        if (positions != null && positions.isArray()) {
            return positions;
        }
        throw new IllegalStateException("Binance " + operation + " response must be an array or contain positions array");
    }

    @Override
    public List<ExchangeSymbol> getExchangeSymbols() throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String exchangeInfoResponse = exchangeClient.get(EXCHANGE_INFO_PATH, Map.of());
        String tickerResponse = exchangeClient.get(TICKER_24HR_PATH, Map.of());
        JsonNode root = readSuccessTree(exchangeInfoResponse, "exchange info");
        JsonNode tickerRoot = readSuccessTree(tickerResponse, "24hr ticker");
        JsonNode symbolsNode = requiredNode(root, "symbols", "exchange info");
        Map<String, TickerSummary> tickerBySymbol = tickerSummaries(tickerRoot);
        List<ExchangeSymbolCandidate> candidates = new ArrayList<>();
        for (JsonNode symbolNode : symbolsNode) {
            String symbol = requiredText(symbolNode, "symbol", "exchange info symbol").toUpperCase(Locale.ROOT);
            String status = requiredText(symbolNode, "status", "exchange info symbol");
            String quoteAsset = requiredText(symbolNode, "quoteAsset", "exchange info symbol");
            String contractType = requiredText(symbolNode, "contractType", "exchange info symbol");
            if (!isSupportedUsdtPerpetual(symbol, status, quoteAsset, contractType)) {
                continue;
            }
            BigDecimal quantityStepSize = filterDecimal(symbolNode, "LOT_SIZE", "stepSize");
            BigDecimal minQuantity = filterDecimal(symbolNode, "LOT_SIZE", "minQty");
            BigDecimal priceTickSize = filterDecimal(symbolNode, "PRICE_FILTER", "tickSize");
            TickerSummary ticker = tickerBySymbol.getOrDefault(symbol, TickerSummary.empty());
            candidates.add(new ExchangeSymbolCandidate(
                    symbol,
                    requiredText(symbolNode, "baseAsset", "exchange info symbol"),
                    quoteAsset,
                    status,
                    quantityStepSize,
                    minQuantity,
                    priceTickSize,
                    ticker.quoteVolume(),
                    ticker.lastPrice()
            ));
        }
        candidates.sort(Comparator.comparing(ExchangeSymbolCandidate::quoteVolume).reversed()
                .thenComparing(ExchangeSymbolCandidate::symbol));

        List<ExchangeSymbol> symbols = new ArrayList<>(candidates.size());
        for (int index = 0; index < candidates.size(); index++) {
            ExchangeSymbolCandidate candidate = candidates.get(index);
            symbols.add(new ExchangeSymbol(
                    candidate.symbol(),
                    candidate.baseAsset(),
                    candidate.quoteAsset(),
                    candidate.status(),
                    candidate.quantityStepSize(),
                    candidate.minQuantity(),
                    candidate.priceTickSize(),
                    candidate.quoteVolume(),
                    candidate.lastPrice(),
                    index + 1
            ));
        }
        return symbols;
    }

    @Override
    public SymbolPrice getPrice(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(PRICE_PATH, params);
        JsonNode priceNode = readSuccessTree(response, "symbol price");
        return new SymbolPrice(requiredText(priceNode, "symbol", "symbol price"), requiredDecimal(priceNode, "price", "symbol price"));
    }

    @Override
    public Order order(Map<String, String> params) {
        try {
            String order = exchangeClient.post(ORDER_PATH, params);
            JsonNode response = objectMapper.readTree(order);
            if (response.has("code") && response.has("msg")) {
                throw new IllegalStateException("Binance order failed code=" + response.get("code").asString()
                        + ", msg=" + response.get("msg").asString());
            }
            return objectMapper.readValue(order, Order.class);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Order orderDetail(Map<String, String> params) {
        try {
            String order = exchangeClient.get(ORDER_PATH, params);
            assertBinanceSuccess(order, "order detail");
            return objectMapper.readValue(order, Order.class);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Order cancelOrder(Map<String, String> params) {
        try {
            String order = exchangeClient.delete(ORDER_PATH, params);
            assertBinanceSuccess(order, "order cancel");
            return objectMapper.readValue(order, Order.class);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void changeLeverage(Map<String, String> params) {
        try {
            String response = exchangeClient.post(LEVERAGE_PATH, params);
            assertBinanceSuccess(response, "leverage change");
        } catch (NoSuchAlgorithmException | InvalidKeyException | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(valueNode.asString());
    }

    private BigDecimal decimalAny(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valueNode = node.get(fieldName);
            if (valueNode != null && !valueNode.isNull()) {
                return new BigDecimal(valueNode.asString());
            }
        }
        return BigDecimal.ZERO;
    }

    private String textOrDefault(JsonNode node, String fieldName, String defaultValue) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return defaultValue;
        }
        return valueNode.asString();
    }

    private JsonNode readSuccessTree(String response, String operation) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        throwIfBinanceError(root, operation);
        return root;
    }

    private void assertBinanceSuccess(String response, String operation) throws IOException {
        throwIfBinanceError(objectMapper.readTree(response), operation);
    }

    private void throwIfBinanceError(JsonNode root, String operation) {
        if (root != null && root.has("code") && root.has("msg")) {
            throw new IllegalStateException("Binance " + operation + " failed code="
                    + root.get("code").asString() + ", msg=" + root.get("msg").asString());
        }
    }

    private JsonNode requiredNode(JsonNode node, String fieldName, String operation) {
        JsonNode valueNode = node == null ? null : node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            throw new IllegalStateException("Binance " + operation + " response missing field: " + fieldName);
        }
        return valueNode;
    }

    private BigDecimal requiredDecimal(JsonNode node, String fieldName, String operation) {
        return new BigDecimal(requiredNode(node, fieldName, operation).asString());
    }

    private String requiredText(JsonNode node, String fieldName, String operation) {
        return requiredNode(node, fieldName, operation).asString();
    }

    private BigDecimal filterDecimal(JsonNode symbolNode, String filterType, String fieldName) {
        JsonNode filters = symbolNode.get("filters");
        if (filters == null || !filters.isArray()) {
            return null;
        }
        for (JsonNode filter : filters) {
            JsonNode type = filter.get("filterType");
            if (type != null && filterType.equalsIgnoreCase(type.asString())) {
                JsonNode value = filter.get(fieldName);
                return value == null || value.isNull() ? null : new BigDecimal(value.asString());
            }
        }
        return null;
    }

    private Map<String, TickerSummary> tickerSummaries(JsonNode tickerRoot) {
        if (tickerRoot == null || !tickerRoot.isArray()) {
            throw new IllegalStateException("Binance 24hr ticker response must be an array");
        }
        Map<String, TickerSummary> summaries = new HashMap<>();
        for (JsonNode tickerNode : tickerRoot) {
            String symbol = textOrDefault(tickerNode, "symbol", "").toUpperCase(Locale.ROOT);
            if (symbol.isBlank()) {
                log.warn("Binance 24hr ticker row skipped because symbol is missing row={}", tickerNode);
                continue;
            }
            summaries.put(symbol, new TickerSummary(
                    decimalAny(tickerNode, "quoteVolume", "quoteVolumeAsset", "volume"),
                    decimalAny(tickerNode, "lastPrice", "price", "closePrice")
            ));
        }
        return summaries;
    }

    private boolean isSupportedUsdtPerpetual(String symbol, String status, String quoteAsset, String contractType) {
        return "TRADING".equalsIgnoreCase(status)
                && "USDT".equalsIgnoreCase(quoteAsset)
                && "PERPETUAL".equalsIgnoreCase(contractType)
                && symbol.endsWith("USDT")
                && !symbol.contains("TEST");
    }

    private record TickerSummary(BigDecimal quoteVolume, BigDecimal lastPrice) {
        private static TickerSummary empty() {
            return new TickerSummary(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }

    private record ExchangeSymbolCandidate(
            String symbol,
            String baseAsset,
            String quoteAsset,
            String status,
            BigDecimal quantityStepSize,
            BigDecimal minQuantity,
            BigDecimal priceTickSize,
            BigDecimal quoteVolume,
            BigDecimal lastPrice
    ) {
    }
}
