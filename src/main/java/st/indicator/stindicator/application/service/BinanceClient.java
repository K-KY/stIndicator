package st.indicator.stindicator.application.service;

import com.java.calculator.AtrCalculator;
import com.java.candle.Candle;
import org.springframework.stereotype.Service;
import st.indicator.stindicator.application.dto.CandleCommand;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.application.exception.BalanceFetchFailException;
import st.indicator.stindicator.application.exception.CandleFetchFailException;
import st.indicator.stindicator.domain.entity.Order;

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
    public BigDecimal getBalance() {
        long currentTimeMillis = System.currentTimeMillis();
        try {
            return exchangeConnector.getBalance(Map.of("timestamp", String.valueOf(currentTimeMillis)));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new BalanceFetchFailException(e, "지갑 조회 실패");
        }
    }

    @Override
    public List<Candle> getCandles(CandleCommand dto) {
        try {
            return exchangeConnector.getCandles(
                    Map.of(
                            "symbol", dto.getSymbol(),
                            "interval", dto.getInterval(),
                            "limit", dto.getLimit()
                    )
            );
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new CandleFetchFailException(e, "캔들 조회 실패");
        }
    }

    @Override
    public BigDecimal getAtr(CandleCommand dto) {
        AtrCalculator atrCalculator = new AtrCalculator();
        atrCalculator.execute(getCandles(dto), 14);
        return null;
    }

    @Override
    public Order order(OrderCommand dto) {
        long timeMillis = System.currentTimeMillis();

        return exchangeConnector.order(Map.of(
                "symbol", dto.getSymbol(),
                "side", dto.getSide(),
                "type", dto.getType(),
                "timeInForce", dto.getTimeInForce(),
                "quantity", dto.getQuantity(),
                "price", dto.getPrice(),
                "timestamp", String.valueOf(timeMillis)
                ));
    }

    @Override
    public void getOrders() {

    }

    @Override
    public Order getOrderDetail(String symbol, String orderId) {
        return exchangeConnector.orderDetail(Map.of("symbol", symbol, "orderId", orderId));
    }


    @Override
    public void getPositions() {

    }
}
