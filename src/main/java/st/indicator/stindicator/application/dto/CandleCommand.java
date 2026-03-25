package st.indicator.stindicator.application.dto;

public class CandleCommand {
    String symbol;
    String interval;
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
