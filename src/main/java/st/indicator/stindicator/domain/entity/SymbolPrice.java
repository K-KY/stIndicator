package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;

/**
 * 특정 심볼의 현재 조회 가격 엔티티
 * 주문 계산과 가격 표시의 기준값
 */
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
