package st.indicator.stindicator.application.dto;

public class CandleCommand {
    // 거래소에서 캔들을 조회할 대상 심볼
    String symbol;
    // 거래소 kline 조회에 사용할 interval 값
    String interval;
    // 거래소 kline 조회 limit 파라미터 값
    String limit;

    public CandleCommand(String symbol, String interval, String limit) {
        this.symbol = symbol;
        this.interval = interval;
        this.limit = limit;
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
}
