package st.indicator.stindicator.presentation.dto;

public class SymbolMonitorDto {
    private String type;
    private String symbol;
    private String interval;

    public SymbolMonitorDto(String type, String symbol, String interval) {
        this.type = type;
        this.symbol = symbol;
        this.interval = interval;
    }

    public String getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }
}
