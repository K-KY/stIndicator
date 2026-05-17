package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.CandleCommand;

public class CandleRequestDto {
    // 조회할 거래 심볼. Binance Futures 심볼 형식 예: BTCUSDT
    String symbol;
    // 조회할 캔들 주기. Binance kline interval 값 예: 1m, 15m, 1h, 4h
    String interval;
    // 몇 개의 캔들을 가져올지 지정하는 조회 개수
    String limit;

    public CandleRequestDto(String symbol, String interval, String limit) {
        this.symbol = symbol;
        this.interval = interval;
        this.limit = limit;
    }

    public CandleRequestDto() {
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public String getLimit() {
        return limit;
    }

    public CandleCommand toCommand() {
        return new CandleCommand(symbol, interval, limit);
    }
}
