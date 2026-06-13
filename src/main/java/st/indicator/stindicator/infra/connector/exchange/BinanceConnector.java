package st.indicator.stindicator.infra.connector.exchange;

import org.springframework.stereotype.Component;
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
    private static final String ACCOUNT_PATH = "https://fapi.binance.com/fapi/v3/account";
    private static final String TOTAL_WALLET_BALANCE = "totalWalletBalance";
    private static final String CANDLE_PATH = "https://fapi.binance.com/fapi/v1/klines";
    private static final String ORDER_PATH = "https://fapi.binance.com/fapi/v1/order";
    private static final String LEVERAGE_PATH = "https://fapi.binance.com/fapi/v1/leverage";
    private static final String POSITION_RISK_PATH = "https://fapi.binance.com/fapi/v2/positionRisk";
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
        String response = exchangeClient.get(POSITION_RISK_PATH, params);
        JsonNode positionsNode = readSuccessTree(response, "position risk");
        List<PositionRisk> positions = new ArrayList<>();
        for (JsonNode positionNode : positionsNode) {
            BigDecimal positionAmt = decimal(positionNode, "positionAmt");
            if (positionAmt.signum() == 0) {
                continue;
            }
            positions.add(new PositionRisk(
                    positionNode.get("symbol").asString(),
                    positionAmt,
                    decimal(positionNode, "entryPrice"),
                    decimal(positionNode, "markPrice"),
                    decimal(positionNode, "unRealizedProfit"),
                    decimal(positionNode, "leverage"),
                    positionNode.get("positionSide").asString()
            ));
        }
        return positions;
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
            String symbol = requiredText(tickerNode, "symbol", "24hr ticker").toUpperCase(Locale.ROOT);
            summaries.put(symbol, new TickerSummary(
                    requiredDecimal(tickerNode, "quoteVolume", "24hr ticker"),
                    requiredDecimal(tickerNode, "lastPrice", "24hr ticker")
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
