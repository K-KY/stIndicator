package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.AtrOrderCommand;

import java.math.BigDecimal;

public class AtrOrderRequestDto {
    private String symbol;
    private String side;
    private String interval;
    private String limit;
    private Integer atrPeriod;
    private BigDecimal riskPercent;
    private BigDecimal atrMultiplier;
    private BigDecimal leverage;
    private String type;
    private String timeInForce;
    private BigDecimal entryPrice;

    public AtrOrderRequestDto() {
    }

    public AtrOrderRequestDto(String symbol, String side, String interval, String limit,
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

    public AtrOrderCommand toCommand() {
        return new AtrOrderCommand(
                symbol,
                side,
                interval == null ? "1h" : interval,
                limit == null ? "150" : limit,
                atrPeriod == null ? 14 : atrPeriod,
                riskPercent == null ? BigDecimal.ONE : riskPercent,
                atrMultiplier == null ? BigDecimal.ONE : atrMultiplier,
                leverage == null ? BigDecimal.ONE : leverage,
                type == null ? "MARKET" : type,
                timeInForce,
                entryPrice
        );
    }
}
