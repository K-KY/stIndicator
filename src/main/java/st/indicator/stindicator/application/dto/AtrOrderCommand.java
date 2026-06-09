package st.indicator.stindicator.application.dto;

import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class AtrOrderCommand {
    // ATR 기반 계산과 실제 주문 실행에 사용할 대상 심볼
    private final String symbol;
    // BUY면 롱 기준, SELL이면 숏 기준으로 손절/목표가를 계산한다
    private final String side;
    // ATR 계산에 사용할 캔들 주기
    private final String interval;
    // ATR 계산에 사용할 캔들 조회 개수
    private final String limit;
    // ATR 계산 기간
    private final Integer atrPeriod;
    // 가용 자산 대비 허용할 리스크 비율
    private final BigDecimal riskPercent;
    // ATR 값에 곱해 손절 거리로 변환하는 배수
    private final BigDecimal atrMultiplier;
    // 필요 증거금 계산에 사용할 레버리지
    private final BigDecimal leverage;
    // 실제 거래소에 보낼 주문 타입
    private final String type;
    // LIMIT 주문일 때만 의미 있는 시간 정책
    private final String timeInForce;
    // 직접 지정한 진입 가격. 없으면 현재가 기준 계산
    private final BigDecimal entryPrice;
    // 손절 조건을 가격 변동률로 볼지 투입 마진 손익률로 볼지 나타낸다
    private final TriggerBasis stopTriggerBasis;
    // 익절 조건을 가격 변동률로 볼지 투입 마진 손익률로 볼지 나타낸다
    private final TriggerBasis takeProfitTriggerBasis;

    public AtrOrderCommand(String symbol, String side, String interval, String limit,
                           Integer atrPeriod, BigDecimal riskPercent, BigDecimal atrMultiplier,
                           BigDecimal leverage, String type, String timeInForce,
                           BigDecimal entryPrice) {
        this(symbol, side, interval, limit, atrPeriod, riskPercent, atrMultiplier, leverage,
                type, timeInForce, entryPrice, TriggerBasis.PRICE_PERCENT, TriggerBasis.PRICE_PERCENT);
    }

    public AtrOrderCommand(String symbol, String side, String interval, String limit,
                           Integer atrPeriod, BigDecimal riskPercent, BigDecimal atrMultiplier,
                           BigDecimal leverage, String type, String timeInForce,
                           BigDecimal entryPrice, TriggerBasis stopTriggerBasis,
                           TriggerBasis takeProfitTriggerBasis) {
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
        this.stopTriggerBasis = stopTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : stopTriggerBasis;
        this.takeProfitTriggerBasis = takeProfitTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : takeProfitTriggerBasis;
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

    public TriggerBasis getStopTriggerBasis() {
        return stopTriggerBasis;
    }

    public TriggerBasis getTakeProfitTriggerBasis() {
        return takeProfitTriggerBasis;
    }
}
