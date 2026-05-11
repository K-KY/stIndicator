package st.indicator.stindicator.domain.entity;

public class ExchangeSymbol {
    private final String symbol;
    private final String baseAsset;
    private final String quoteAsset;
    private final String status;

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status) {
        this.symbol = symbol;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getBaseAsset() {
        return baseAsset;
    }

    public String getQuoteAsset() {
        return quoteAsset;
    }

    public String getStatus() {
        return status;
    }
}
