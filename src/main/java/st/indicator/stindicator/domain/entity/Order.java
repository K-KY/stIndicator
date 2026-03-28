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
    BigDecimal cumQtx,y;
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
}
