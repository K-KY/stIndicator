package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import st.indicator.stindicator.application.dto.CandleCommand;
import st.indicator.stindicator.application.dto.OrderCommand;
import st.indicator.stindicator.domain.entity.Order;

import java.math.BigDecimal;
import java.util.List;

public interface ClientService {
    BigDecimal getBalance();//지갑 조회
    List<Candle> getCandles(CandleCommand dto);//캔들 데이터 조회
    BigDecimal getAtr(CandleCommand dto);
    Order order(OrderCommand dto);
    void getOrders();//주문 목록 조회
    void getPositions();//보유 포지션 목록 조회
}
