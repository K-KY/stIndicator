package st.indicator.stindicator.domain.entity;


import java.math.BigDecimal;

public class Order {
    // 거래소가 발급한 주문 고유 ID
    String orderId;
    // 주문이 속한 거래 심볼
    String symbol;
    // NEW, FILLED, CANCELED 같은 거래소 주문 상태
    String status;
    // 클라이언트가 지정한 주문 ID. 지정하지 않으면 거래소가 생성
    String clientOrderId;
    // 지정가 주문 가격. 시장가 주문이면 0일 수 있다
    BigDecimal price;
    // 체결 평균 가격
    BigDecimal avgPrice;
    // 최초 주문 수량
    BigDecimal origQty;
    // 지금까지 실제 체결된 수량
    BigDecimal executedQty;
    // 누적 체결 수량
    BigDecimal cumQty;
    // 누적 체결 금액
    BigDecimal cumQuote;
    // 주문 유지 정책. 예: GTC
    String timeInForce;
    // 주문 타입. MARKET, LIMIT 등
    String type;
    // reduceOnly 주문 여부. 기존 포지션 축소/청산 전용인지 나타낸다
    Boolean reduceOnly;
    // closePosition 전량 청산 플래그 여부
    Boolean closePosition;
    // BUY 또는 SELL
    String side;
    // 포지션 방향. Hedge 모드에서 LONG/SHORT 등을 표현
    String positionSide;
    // 조건부 주문의 스탑 가격
    BigDecimal stopPrice;
    // stop 기준 가격 종류. 예: MARK_PRICE
    String workingType;
    // 가격 보호 옵션 사용 여부
    Boolean priceProtect;
    // 원본 주문 타입
    String origType;
    // 거래소 내부 가격 매칭 정책
    String priceMatch;
    // 자기 자신 주문 체결 방지 정책
    String selfTradePreventionMode;
    // GTD 주문 만료 시각
    Integer goodTillDate;
    // 거래소가 응답한 마지막 업데이트 시각
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
