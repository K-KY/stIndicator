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
    private final java.math.BigDecimal quoteVolume;
    private final java.math.BigDecimal lastPrice;
    private final Integer rank;

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status) {
        this(symbol, baseAsset, quoteAsset, status, null, null, null, null, null, null);
    }

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status,
                          java.math.BigDecimal quantityStepSize,
                          java.math.BigDecimal minQuantity,
                          java.math.BigDecimal priceTickSize) {
        this(symbol, baseAsset, quoteAsset, status, quantityStepSize, minQuantity, priceTickSize,
                null, null, null);
    }

    public ExchangeSymbol(String symbol, String baseAsset, String quoteAsset, String status,
                          java.math.BigDecimal quantityStepSize,
                          java.math.BigDecimal minQuantity,
                          java.math.BigDecimal priceTickSize,
                          java.math.BigDecimal quoteVolume,
                          java.math.BigDecimal lastPrice,
                          Integer rank) {
        this.symbol = symbol;
        this.baseAsset = baseAsset;
        this.quoteAsset = quoteAsset;
        this.status = status;
        this.quantityStepSize = quantityStepSize;
        this.minQuantity = minQuantity;
        this.priceTickSize = priceTickSize;
        this.quoteVolume = quoteVolume;
        this.lastPrice = lastPrice;
        this.rank = rank;
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

    public java.math.BigDecimal getQuoteVolume() {
        return quoteVolume;
    }

    public java.math.BigDecimal getLastPrice() {
        return lastPrice;
    }

    public Integer getRank() {
        return rank;
    }
}
