package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import st.indicator.stindicator.domain.entity.Order;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public interface ExchangeConnector {
    List<Candle> getCandles(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;

    BigDecimal getBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    Order orderDetail(Map<String, String> params);
    Order order(Map<String, String> params);
}