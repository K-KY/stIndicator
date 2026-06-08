package st.indicator.stindicator.domain.entity;

/**
 * 거래소에서 현재 거래 가능한 선물 심볼 정보
 */
public class ExchangeSymbol {
    private final String symbol;
    private final String baseAsset;
    private final String quoteAsset;
    private final String status;
    private final java.math.BigDecimal quantityStepSize;
    private final java.math.BigDecimal minQuantity;
    private final java.math.BigDecimal priceTickSize;

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status) {
        this(symbol, baseAsset, quoteAsset, status, null, null, null);
    }

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status,
                          java.math.BigDecimal quantityStepSize,
                          java.math.BigDecimal minQuantity,
                          java.math.BigDecimal priceTickSize) {
        this.symbol = symbol;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.status = status;
        this.quantityStepSize = quantityStepSize;
        this.minQuantity = minQuantity;
        this.priceTickSize = priceTickSize;
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

    public java.math.BigDecimal getQuantityStepSize() {
        return quantityStepSize;
    }

    public java.math.BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public java.math.BigDecimal getPriceTickSize() {
        return priceTickSize;
    }
}
