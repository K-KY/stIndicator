package st.indicator.stindicator.infra.connector.exchange;

import com.java.candle.Candle;
import com.java.candle.CandleMapper;
import com.java.client.ExchangeClient;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.infra.connector.entity.OrderEntity;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Component
public class BinanceConnector implements ExchangeConnector {
    private static final String CANDLE_PATH = "https://fapi.binance.com/fapi/v1/klines";
    private static final String ACCOUNT_PATH = "https://fapi.binance.com/fapi/v2/account";
    private static final String ORDER_PATH = "https://fapi.binance.com/fapi/v1/order";
    private static final String TOTAL_WALLET_BALANCE = "totalWalletBalance";
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
    public OrderEntity order(Map<String, String> params) {
        try {
            String order = exchangeClient.post(ORDER_PATH, params);
            return objectMapper.readValue(order, OrderEntity.class);
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
    public void orders() {
    }
}
