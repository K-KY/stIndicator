package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.CandleRequestDto;
import st.indicator.stindicator.infra.connector.exchange.ExchangeConnector;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Service
public class BinanceClient implements ClientService {
    private final ExchangeConnector exchangeConnector;
    public BinanceClient(ExchangeConnector exchangeConnector) {
        this.exchangeConnector = exchangeConnector;
    }
    @Override
    public BigDecimal getBalance() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        long currentTimeMillis = System.currentTimeMillis();
        return exchangeConnector.getBalance(Map.of("timestamp", String.valueOf(currentTimeMillis)));
    }

    @Override
    public List<Candle> getCandles(CandleRequestDto dto)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException {
        return exchangeConnector.getCandles(
                Map.of(
                        "symbol", dto.getSymbol(),
                        "interval", dto.getInterval(),
                        "limit", dto.getLimit()
                )
        );
    }

    @Override
    public void buy() {

    }

    @Override
    public void sell() {

    }

    @Override
    public void getOrders() {

    }

    @Override
    public void getPositions() {

    }
}
