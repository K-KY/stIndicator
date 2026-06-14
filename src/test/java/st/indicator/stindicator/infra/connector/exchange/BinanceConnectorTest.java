package st.indicator.stindicator.infra.connector.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.utils.candle.Candle;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BinanceConnectorTest {

    @Autowired
    BinanceConnector binanceConnector;

    @Test
    void getCandles() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        List<Candle> candles = binanceConnector.getCandles(Map.of(
                        "symbol", "BTCUSDT",
                        "interval", "4h",
                        "limit", "50"
                )
        );
        assertThat(candles.getLast()).isInstanceOf(Candle.class);
    }

    @Test
    void getBalances() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        assertThat(binanceConnector.getBalance(Map.of("timestamp", System.currentTimeMillis() + "")))
                .isNotNull();
    }

    @Test
    void order() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", "ETHUSDT");//r
        params.put("side", "BUY");//r
        params.put("type", "LIMIT");//r
        params.put("timeInForce", "GTC");
        params.put("quantity", "1");
        params.put("price", "1500");
        params.put("timestamp", System.currentTimeMillis() + "");

        Order order = binanceConnector.order(params);
        assertThat(order.getOrderId()).isNotNull();
        System.out.println("order.getOrderId() = " + order.getOrderId());
    }

}