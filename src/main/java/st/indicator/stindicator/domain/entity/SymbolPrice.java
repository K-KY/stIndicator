package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;

public class SymbolPrice {
    private final String symbol;
    private final BigDecimal price;

    public SymbolPrice(String symbol, BigDecimal price) {
        this.symbol = symbol;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
