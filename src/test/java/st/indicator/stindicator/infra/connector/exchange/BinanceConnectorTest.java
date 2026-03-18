package st.indicator.stindicator.infra.connector.exchange;

import com.java.candle.Candle;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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

}