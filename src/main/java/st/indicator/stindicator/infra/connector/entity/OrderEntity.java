package st.indicator.stindicator.infra.connector.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class OrderEntity {

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

    String updateTime;
    // 마지막 업데이트 시간 (timestamp)
}