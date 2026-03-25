package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.CandleCommand;

public class CandleRequestDto {
    String symbol;
    String interval;
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
