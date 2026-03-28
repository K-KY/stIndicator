package st.indicator.stindicator.domain.entity;


import java.math.BigDecimal;

public class Order {
    String orderId;
    String symbol;
    String status;
    String clientOrderId;
    BigDecimal price;
    BigDecimal avgPrice;
    BigDecimal origQty;
    BigDecimal executedQty;
    BigDecimal cumQty;
    BigDecimal cumQuote;
    String timeInForce;
    String type;
    Boolean reduceOnly;
    Boolean closePosition;
    String side;
    String positionSide;
    BigDecimal stopPrice;
    String workingType;
    Boolean priceProtect;
    String origType;
    String priceMatch;
    String selfTradePreventionMode;
    Integer goodTillDate;
    String updateTime;

    public Order(String orderId, String symbol,
                 String status, String clientOrderId,
                 BigDecimal price, BigDecimal avgPrice,
                 BigDecimal origQty, BigDecimal executedQty,
                 BigDecimal cumQty,
                 BigDecimal cumQuote, String timeInForce,
                 String type, Boolean reduceOnly,
                 Boolean closePosition, String side,
                 String positionSide, BigDecimal stopPrice,
                 String workingType, Boolean priceProtect,
                 String origType, String priceMatch,
                 String selfTradePreventionMode, Integer goodTillDate,
                 String updateTime) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.status = status;
        this.clientOrderId = clientOrderId;
        this.price = price;
        this.avgPrice = avgPrice;
        this.origQty = origQty;
        this.executedQty = executedQty;
        this.cumQty = cumQty;
        this.cumQuote = cumQuote;
        this.timeInForce = timeInForce;
        this.type = type;
        this.reduceOnly = reduceOnly;
        this.closePosition = closePosition;
        this.side = side;
        this.positionSide = positionSide;
        this.stopPrice = stopPrice;
        this.workingType = workingType;
        this.priceProtect = priceProtect;
        this.origType = origType;
        this.priceMatch = priceMatch;
        this.selfTradePreventionMode = selfTradePreventionMode;
        this.goodTillDate = goodTillDate;
        this.updateTime = updateTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getStatus() {
        return status;
    }

    public String getClientOrderId() {
        return clientOrderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public BigDecimal getOrigQty() {
        return origQty;
    }

    public BigDecimal getExecutedQty() {
        return executedQty;
    }

    public BigDecimal getCumQty() {
        return cumQty;
    }

    public BigDecimal getCumQuote() {
        return cumQuote;
    }

    public String getTimeInForce() {
        return timeInForce;
    }

    public String getType() {
        return type;
    }

    public Boolean getReduceOnly() {
        return reduceOnly;
    }

    public Boolean getClosePosition() {
        return closePosition;
    }

    public String getSide() {
        return side;
    }

    public String getPositionSide() {
        return positionSide;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public String getWorkingType() {
        return workingType;
    }

    public Boolean getPriceProtect() {
        return priceProtect;
    }

    public String getOrigType() {
        return origType;
    }

    public String getPriceMatch() {
        return priceMatch;
    }

    public String getSelfTradePreventionMode() {
        return selfTradePreventionMode;
    }

    public Integer getGoodTillDate() {
        return goodTillDate;
    }

    public String getUpdateTime() {
        return updateTime;
    }
}
