package st.indicator.stindicator.domain.entity;

/**
 * 서비스 관리형 TP/SL 조건을 어떤 기준으로 평가할지 나타낸다.
 * PRICE_PERCENT는 코인 가격 변동률, PNL_PERCENT는 투입 증거금 대비 손익률 기준이다.
 */
public enum TriggerBasis {
    PRICE_PERCENT,
    PNL_PERCENT
}
