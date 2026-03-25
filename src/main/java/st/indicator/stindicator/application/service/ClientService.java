package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import st.indicator.stindicator.application.dto.CandleRequestDto;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public interface ClientService {
    BigDecimal getBalance() throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException;//자산 조회
    List<Candle> getCandles(CandleRequestDto dto) throws IOException, NoSuchAlgorithmException, InvalidKeyException, InterruptedException;//캔들 데이터 조회
    void buy();//구매
    void sell();//판매
    void getOrders();//주문 목록 조회
    void getPositions();//보유 포지션 목록 조회
}
