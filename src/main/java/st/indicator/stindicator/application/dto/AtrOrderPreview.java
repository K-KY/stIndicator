package st.indicator.stindicator.application.dto;

import java.math.BigDecimal;

public class AtrOrderPreview {
    private final String symbol;
    private final String side;
    private final String interval;
    private final Integer atrPeriod;
    private final BigDecimal availableBalance;
    private final BigDecimal entryPrice;
    private final BigDecimal atr;
    private final BigDecimal atrMultiplier;
    private final BigDecimal stopDistance;
    private final BigDecimal riskPercent;
    private final BigDecimal riskAmount;
    private final BigDecimal leverage;
    private final BigDecimal quantity;
    private final BigDecimal notional;
    private final BigDecimal requiredMargin;
    private final BigDecimal stopPrice;
    private final BigDecimal targetPrice;
    private final BigDecimal possibleLoss;
    private final BigDecimal possibleProfit;

    public AtrOrderPreview(String symbol, String side, String interval, Integer atrPeriod,
                           BigDecimal availableBalance, BigDecimal entryPrice, BigDecimal atr,
                           BigDecimal atrMultiplier, BigDecimal stopDistance, BigDecimal riskPercent,
                           BigDecimal riskAmount, BigDecimal leverage, BigDecimal quantity,
                           BigDecimal notional, BigDecimal requiredMargin, BigDecimal stopPrice,
                           BigDecimal targetPrice, BigDecimal possibleLoss, BigDecimal possibleProfit) {
        this.symbol = symbol;
        this.side = side;
        this.interval = interval;
        this.atrPeriod = atrPeriod;
        this.availableBalance = availableBalance;
        this.entryPrice = entryPrice;
        this.atr = atr;
        this.atrMultiplier = atrMultiplier;
        this.stopDistance = stopDistance;
        this.riskPercent = riskPercent;
        this.riskAmount = riskAmount;
        this.leverage = leverage;
        this.quantity = quantity;
        this.notional = notional;
        this.requiredMargin = requiredMargin;
        this.stopPrice = stopPrice;
        this.targetPrice = targetPrice;
        this.possibleLoss = possibleLoss;
        this.possibleProfit = possibleProfit;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getSide() {
        return side;
    }

    public String getInterval() {
        return interval;
    }

    public Integer getAtrPeriod() {
        return atrPeriod;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getAtr() {
        return atr;
    }

    public BigDecimal getAtrMultiplier() {
        return atrMultiplier;
    }

    public BigDecimal getStopDistance() {
        return stopDistance;
    }

    public BigDecimal getRiskPercent() {
        return riskPercent;
    }

    public BigDecimal getRiskAmount() {
        return riskAmount;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public BigDecimal getRequiredMargin() {
        return requiredMargin;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public BigDecimal getPossibleLoss() {
        return possibleLoss;
    }

    public BigDecimal getPossibleProfit() {
        return possibleProfit;
    }
}
