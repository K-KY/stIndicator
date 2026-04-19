package st.indicator.stindicator.presentation.dto;

import java.util.List;

public class SymbolMonitorDto {
    private String type;
    private List<String> symbols;
    private String interval;

    public SymbolMonitorDto(String type, List<String> symbols, String interval) {
        this.type = type;
        this.symbols = symbols;
        this.interval = interval;
    }

    public String getType() {
        return type;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public String getInterval() {
        return interval;
    }
}
