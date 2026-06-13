package st.indicator.stindicator.domain.entity;

/**
 * 관리형 거래가 Binance 실주문인지 내부 트리거 검증용 가상 주문인지 구분한다.
 */
public enum TradeExecutionMode {
    REAL,
    TEST
}
