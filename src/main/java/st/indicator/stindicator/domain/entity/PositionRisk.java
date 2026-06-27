package st.indicator.stindicator.domain.entity;

import java.math.BigDecimal;

/**
 * 사용자가 보유 중인 선물 포지션의 핵심 위험 정보
 * 진입가, 현재가, 수량, 손익, 배율 정보를 함께 전달해 청산과 모니터링에 사용
 */
public class PositionRisk {
    private final String symbol;
    private final BigDecimal positionAmt;
    private final BigDecimal entryPrice;
    private final BigDecimal markPrice;
    private final BigDecimal unrealizedProfit;
    private final BigDecimal leverage;
    private final BigDecimal liquidationPrice;
    private final BigDecimal notional;
    private final BigDecimal positionInitialMargin;
    private final String positionSide;

    public PositionRisk(String symbol, BigDecimal positionAmt, BigDecimal entryPrice,
                        BigDecimal markPrice, BigDecimal unrealizedProfit,
                        BigDecimal leverage, String positionSide) {
        this(symbol, positionAmt, entryPrice, markPrice, unrealizedProfit, leverage, null, null, null, positionSide);
    }

    public PositionRisk(String symbol, BigDecimal positionAmt, BigDecimal entryPrice,
                        BigDecimal markPrice, BigDecimal unrealizedProfit,
                        BigDecimal leverage, BigDecimal liquidationPrice, String positionSide) {
        this(symbol, positionAmt, entryPrice, markPrice, unrealizedProfit, leverage, liquidationPrice, null, null, positionSide);
    }

    public PositionRisk(String symbol, BigDecimal positionAmt, BigDecimal entryPrice,
                        BigDecimal markPrice, BigDecimal unrealizedProfit,
                        BigDecimal leverage, BigDecimal liquidationPrice,
                        BigDecimal notional, String positionSide) {
        this(symbol, positionAmt, entryPrice, markPrice, unrealizedProfit, leverage, liquidationPrice, notional, null, positionSide);
    }

    public PositionRisk(String symbol, BigDecimal positionAmt, BigDecimal entryPrice,
                        BigDecimal markPrice, BigDecimal unrealizedProfit,
                        BigDecimal leverage, BigDecimal liquidationPrice,
                        BigDecimal notional, BigDecimal positionInitialMargin, String positionSide) {
        this.symbol = symbol;
        this.positionAmt = positionAmt;
        this.entryPrice = entryPrice;
        this.markPrice = markPrice;
        this.unrealizedProfit = unrealizedProfit;
        this.leverage = leverage;
        this.liquidationPrice = liquidationPrice;
        this.notional = notional;
        this.positionInitialMargin = positionInitialMargin;
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

    public BigDecimal getLiquidationPrice() {
        return liquidationPrice;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public BigDecimal getPositionInitialMargin() {
        return positionInitialMargin;
    }

    public String getPositionSide() {
        return positionSide;
    }
}
