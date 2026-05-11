package st.indicator.stindicator.domain.entity;

/**
 * 거래소에서 현재 거래 가능한 선물 심볼 정보
 */
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
