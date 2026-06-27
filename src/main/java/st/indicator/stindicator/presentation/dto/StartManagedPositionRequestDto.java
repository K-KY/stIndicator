package st.indicator.stindicator.presentation.dto;

public class StartManagedPositionRequestDto extends UpdateManagedPositionModeRequestDto {
    private String symbol;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }
}
