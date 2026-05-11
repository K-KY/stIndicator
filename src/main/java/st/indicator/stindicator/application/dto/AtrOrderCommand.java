package st.indicator.stindicator.application.dto;

import java.math.BigDecimal;

public class AtrOrderCommand {
    private final String symbol;
    private final String side;
    private final String interval;
    private final String limit;
    private final Integer atrPeriod;
    private final BigDecimal riskPercent;
    private final BigDecimal atrMultiplier;
    private final BigDecimal leverage;
    private final String type;
    private final String timeInForce;
    private final BigDecimal entryPrice;

    public AtrOrderCommand(String symbol, String side, String interval, String limit,
                           Integer atrPeriod, BigDecimal riskPercent, BigDecimal atrMultiplier,
                           BigDecimal leverage, String type, String timeInForce,
                           BigDecimal entryPrice) {
        this.symbol = symbol;
        this.side = side;
        this.interval = interval;
        this.limit = limit;
        this.atrPeriod = atrPeriod;
        this.riskPercent = riskPercent;
        this.atrMultiplier = atrMultiplier;
        this.leverage = leverage;
        this.type = type;
        this.timeInForce = timeInForce;
        this.entryPrice = entryPrice;
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

    public String getLimit() {
        return limit;
    }

    public Integer getAtrPeriod() {
        return atrPeriod;
    }

    public BigDecimal getRiskPercent() {
        return riskPercent;
    }

    public BigDecimal getAtrMultiplier() {
        return atrMultiplier;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public String getType() {
        return type;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }
}
