package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;

public class PositionRisk {
    private final String symbol;
    private final BigDecimal positionAmt;
    private final BigDecimal entryPrice;
    private final BigDecimal markPrice;
    private final BigDecimal unrealizedProfit;
    private final BigDecimal leverage;
    private final String positionSide;

    public PositionRisk(String symbol, BigDecimal positionAmt, BigDecimal entryPrice,
                        BigDecimal markPrice, BigDecimal unrealizedProfit,
                        BigDecimal leverage, String positionSide) {
        this.symbol = symbol;
        this.positionAmt = positionAmt;
        this.entryPrice = entryPrice;
        this.markPrice = markPrice;
        this.unrealizedProfit = unrealizedProfit;
        this.leverage = leverage;
        this.positionSide = positionSide;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getPositionAmt() {
        return positionAmt;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getMarkPrice() {
        return markPrice;
    }

    public BigDecimal getUnrealizedProfit() {
        return unrealizedProfit;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public String getPositionSide() {
        return positionSide;
    }
}
