package st.indicator.stindicator.infra.connector.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import st.indicator.stindicator.domain.entity.Order;

import java.math.BigDecimal;

@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderEntity {
    public OrderEntity() {
    }

    public OrderEntity(String orderId, String symbol, String status,
                       String clientOrderId, BigDecimal price, BigDecimal avgPrice,
                       BigDecimal origQty, BigDecimal executedQty, BigDecimal cumQty,
                       BigDecimal cumQuote, String timeInForce, String type,
                       Boolean reduceOnly, Boolean closePosition, String side,
                       String positionSide, BigDecimal stopPrice, String workingType,
                       Boolean priceProtect, String origType, String priceMatch,
                       String selfTradePreventionMode, Integer goodTillDate, String updateTime) {
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

    public OrderEntity(Order order) {
        this.orderId = order.getOrderId();
        this.symbol = order.getSymbol();
        this.status = order.getStatus();
        this.clientOrderId = order.getClientOrderId();
        this.price = order.getPrice();
        this.avgPrice = order.getAvgPrice();
        this.origQty = order.getOrigQty();
        this.clientOrderId = order.getClientOrderId();
        this.executedQty = order.getExecutedQty();
        this.cumQty = order.getCumQty();
        this.cumQuote = order.getCumQuote();
        this.timeInForce = order.getTimeInForce();
        this.type = order.getType();
        this.reduceOnly = order.getReduceOnly();
        this.closePosition = order.getClosePosition();
        this.side = order.getSide();
        this.positionSide = order.getPositionSide();
        this.stopPrice = order.getStopPrice();
        this.workingType = order.getWorkingType();
        this.priceProtect = order.getPriceProtect();
        this.origType = order.getOrigType();
        this.priceMatch = order.getPriceMatch();
        this.selfTradePreventionMode = order.getSelfTradePreventionMode();
        this.goodTillDate = order.getGoodTillDate();
        this.updateTime = order.getUpdateTime();
    }

    @Id
    String orderId;
    // 주문 고유 ID (바이낸스가 생성)
    // 취소 / 조회할 때 필수

    String symbol;
    // 거래 심볼 (ex: BTCUSDT)

    String status;
    // 주문 상태
    // NEW / PARTIALLY_FILLED / FILLED / CANCELED

    @Column(unique = true, nullable = false)
    String clientOrderId;
    // 내가 직접 넣은 주문 ID
    // 중복 방지, 추적용 (자동매매 필수)

    @Column(nullable = false)
    BigDecimal price;
    // 주문 가격 (LIMIT일 때만 의미 있음)

    BigDecimal avgPrice;
    // 실제 평균 체결 가격
    // MARKET 주문은 이 값이 중요

    BigDecimal origQty;
    // 처음 주문한 수량

    BigDecimal executedQty;
    // 지금까지 체결된 수량

    BigDecimal cumQty;
    // 누적 체결 수량 (executedQty랑 거의 동일 개념)

    BigDecimal cumQuote;
    // 누적 체결 금액 (가격 * 수량)

    String timeInForce;
    // 주문 유지 방식
    // GTC (취소 전까지 유지)
    // IOC (즉시 체결 안되면 취소)
    // FOK (전량 체결 아니면 취소)

    String type;
    // 주문 타입
    // LIMIT / MARKET / STOP / STOP_MARKET / TAKE_PROFIT 등

    Boolean reduceOnly;
    // true면 포지션 줄이기 전용 (청산용)
    // 신규 진입 막고 기존 포지션만 감소

    Boolean closePosition;
    // true면 포지션 전체 청산
    // (STOP_MARKET에서 많이 씀)

    String side;
    // BUY / SELL
    // BUY = 롱 진입 / 숏 청산
    // SELL = 숏 진입 / 롱 청산

    String positionSide;
    // BOTH / LONG / SHORT
    // Hedge Mode에서 중요

    BigDecimal stopPrice;
    // 트리거 가격 (조건부 주문에서 사용)
    // ex: 손절 가격

    String workingType;
    // 트리거 기준 가격
    // MARK_PRICE / CONTRACT_PRICE

    Boolean priceProtect;
    // 급격한 가격 변동 보호 옵션 (보통 false)

    String origType;
    // 원래 주문 타입 (내부 추적용)

    String priceMatch;
    // 가격 매칭 방식 (거의 안 씀, 고급 옵션)

    String selfTradePreventionMode;
    // 자기 자신과 체결 방지 옵션

    Integer goodTillDate;
    // GTD 주문 만료 시간 (거의 안 씀)

    // 마지막 업데이트 시간 (timestamp)
    String updateTime;

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