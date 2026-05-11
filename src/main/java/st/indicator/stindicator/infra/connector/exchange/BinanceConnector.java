package st.indicator.stindicator.infra.connector.exchange;

import com.java.candle.Candle;
import com.java.candle.CandleMapper;
import com.java.client.ExchangeClient;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.application.service.ExchangeConnector;
import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class BinanceConnector implements ExchangeConnector {
    private static final String CANDLE_PATH = "https://fapi.binance.com/fapi/v1/klines";
    private static final String ACCOUNT_PATH = "https://fapi.binance.com/fapi/v2/account";
    private static final String ORDER_PATH = "https://fapi.binance.com/fapi/v1/order";
    private static final String POSITION_RISK_PATH = "https://fapi.binance.com/fapi/v2/positionRisk";
    private static final String EXCHANGE_INFO_PATH = "https://fapi.binance.com/fapi/v1/exchangeInfo";
    private static final String PRICE_PATH = "https://fapi.binance.com/fapi/v1/ticker/price";
    private static final String TOTAL_WALLET_BALANCE = "totalWalletBalance";
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
        return candleMapper.map(s);
    }

    public BigDecimal getBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String s = exchangeClient.get(ACCOUNT_PATH, params);
        return BigDecimal.valueOf(objectMapper.readTree(s).get(TOTAL_WALLET_BALANCE).asDouble());
    }

    @Override
    public BigDecimal getAvailableBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String s = exchangeClient.get(ACCOUNT_PATH, params);
        return BigDecimal.valueOf(objectMapper.readTree(s).get(AVAILABLE_BALANCE).asDouble());
    }

    @Override
    public List<AssetBalance> getAssets(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(ACCOUNT_PATH, params);
        JsonNode assetsNode = objectMapper.readTree(response).get("assets");
        List<AssetBalance> assets = new ArrayList<>();
        for (JsonNode assetNode : assetsNode) {
            BigDecimal walletBalance = decimal(assetNode, "walletBalance");
            BigDecimal availableBalance = decimal(assetNode, "availableBalance");
            BigDecimal unrealizedProfit = decimal(assetNode, "unrealizedProfit");
            if (walletBalance.signum() == 0 && availableBalance.signum() == 0 && unrealizedProfit.signum() == 0) {
                continue;
            }
            assets.add(new AssetBalance(
                    assetNode.get("asset").asText(),
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
        JsonNode positionsNode = objectMapper.readTree(response);
        List<PositionRisk> positions = new ArrayList<>();
        for (JsonNode positionNode : positionsNode) {
            BigDecimal positionAmt = decimal(positionNode, "positionAmt");
            if (positionAmt.signum() == 0) {
                continue;
            }
            positions.add(new PositionRisk(
                    positionNode.get("symbol").asText(),
                    positionAmt,
                    decimal(positionNode, "entryPrice"),
                    decimal(positionNode, "markPrice"),
                    decimal(positionNode, "unRealizedProfit"),
                    decimal(positionNode, "leverage"),
                    positionNode.get("positionSide").asText()
            ));
        }
        return positions;
    }

    @Override
    public List<ExchangeSymbol> getExchangeSymbols() throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(EXCHANGE_INFO_PATH, Map.of());
        JsonNode symbolsNode = objectMapper.readTree(response).get("symbols");
        List<ExchangeSymbol> symbols = new ArrayList<>();
        for (JsonNode symbolNode : symbolsNode) {
            if (!"TRADING".equalsIgnoreCase(symbolNode.get("status").asText())) {
                continue;
            }
            symbols.add(new ExchangeSymbol(
                    symbolNode.get("symbol").asText(),
                    symbolNode.get("baseAsset").asText(),
                    symbolNode.get("quoteAsset").asText(),
                    symbolNode.get("status").asText()
            ));
        }
        return symbols;
    }

    @Override
    public SymbolPrice getPrice(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException {
        String response = exchangeClient.get(PRICE_PATH, params);
        JsonNode priceNode = objectMapper.readTree(response);
        return new SymbolPrice(priceNode.get("symbol").asText(), decimal(priceNode, "price"));
    }

    @Override
    public Order order(Map<String, String> params) {
        try {
            String order = exchangeClient.post(ORDER_PATH, params);
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

    private BigDecimal decimal(JsonNode node, String fieldName) {
        JsonNode valueNode = node.get(fieldName);
        if (valueNode == null || valueNode.isNull()) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(valueNode.asText());
    }
}
