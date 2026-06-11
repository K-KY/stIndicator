package st.indicator.stindicator.application.service;

import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;
import st.indicator.stindicator.domain.utils.candle.Candle;

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
    BigDecimal getAvailableBalance(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    List<AssetBalance> getAssets(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    List<PositionRisk> getPositions(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    List<ExchangeSymbol> getExchangeSymbols() throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    SymbolPrice getPrice(Map<String, String> params) throws IOException, NoSuchAlgorithmException,
            InvalidKeyException, InterruptedException;
    Order orderDetail(Map<String, String> params);
    Order order(Map<String, String> params);
    Order cancelOrder(Map<String, String> params);
}
