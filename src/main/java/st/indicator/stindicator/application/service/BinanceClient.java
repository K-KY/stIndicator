package st.indicator.stindicator.application.service;

import org.springframework.stereotype.Service;
import com.java.candle.Candle;
import st.indicator.stindicator.application.dto.CandleCommand;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.application.exception.BalanceFetchFailException;
import st.indicator.stindicator.application.exception.CandleFetchFailException;
import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class BinanceClient implements ClientService {
    private final ExchangeConnector exchangeConnector;
    private final AtrPositionSizingService atrPositionSizingService;

    public BinanceClient(ExchangeConnector exchangeConnector, AtrPositionSizingService atrPositionSizingService) {
        this.exchangeConnector = exchangeConnector;
        this.atrPositionSizingService = atrPositionSizingService;
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
        return atrPositionSizingService.calculateAtr(getCandles(dto), 14);
    }

    @Override
    public Order order(OrderCommand dto) {
        return placeOrder(dto.getSymbol(), dto.getSide(), dto.getType(), dto.getTimeInForce(),
                dto.getQuantity(), dto.getPrice(), false);
    }

    @Override
    public List<AssetBalance> getAssets() {
        long currentTimeMillis = System.currentTimeMillis();
        try {
            return exchangeConnector.getAssets(Map.of("timestamp", String.valueOf(currentTimeMillis)));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new BalanceFetchFailException(e, "자산 목록 조회 실패");
        }
    }

    @Override
    public List<ExchangeSymbol> getExchangeSymbols() {
        try {
            return exchangeConnector.getExchangeSymbols();
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("거래소 심볼 목록 조회 실패", e);
        }
    }

    @Override
    public SymbolPrice getPrice(String symbol) {
        try {
            return exchangeConnector.getPrice(Map.of("symbol", symbol));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("현재가 조회 실패", e);
        }
    }

    @Override
    public void getOrders() {

    }

    @Override
    public Order getOrderDetail(String symbol, String orderId) {
        return exchangeConnector.orderDetail(Map.of("symbol", symbol, "orderId", orderId));
    }


    @Override
    public List<PositionRisk> getPositions() {
        long currentTimeMillis = System.currentTimeMillis();
        try {
            return exchangeConnector.getPositions(Map.of("timestamp", String.valueOf(currentTimeMillis)));
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | InterruptedException e) {
            throw new RuntimeException("포지션 조회 실패", e);
        }
    }

    private Order placeOrder(String symbol, String side, String type, String timeInForce,
                             String quantity, String price, boolean reduceOnly) {
        long timeMillis = System.currentTimeMillis();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("symbol", symbol);
        params.put("side", side == null ? "BUY" : side.toUpperCase(Locale.ROOT));
        params.put("type", type == null ? "MARKET" : type.toUpperCase(Locale.ROOT));
        params.put("quantity", quantity);
        params.put("timestamp", String.valueOf(timeMillis));

        if (timeInForce != null && !timeInForce.isBlank()) {
            params.put("timeInForce", timeInForce.toUpperCase(Locale.ROOT));
        }
        if (price != null && !price.isBlank()) {
            params.put("price", price);
        }
        if (reduceOnly) {
            params.put("reduceOnly", "true");
        }

        return exchangeConnector.order(params);
    }
}
