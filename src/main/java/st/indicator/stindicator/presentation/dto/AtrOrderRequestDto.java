package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.AtrOrderCommand;

import java.math.BigDecimal;

public class AtrOrderRequestDto {
    // ATR 기준 주문을 계산하고 실행할 대상 심볼
    private String symbol;
    // 주문 방향. BUY는 롱 기준 계산, SELL은 숏 기준 계산
    private String side;
    // ATR 계산에 사용할 캔들 주기. 예: 1m, 15m, 1h, 4h
    private String interval;
    // ATR 계산용으로 조회할 캔들 개수. period보다 충분히 큰 값을 주는 용도
    private String limit;
    // ATR 기간. True Range 평균을 계산할 캔들 수
    private Integer atrPeriod;
    // 가용 자산 대비 이번 주문에 허용할 리스크 비율. 1이면 가용 자산의 1%
    private BigDecimal riskPercent;
    // 손절 거리 계산에 사용할 ATR 배수. stopDistance = ATR x atrMultiplier
    private BigDecimal atrMultiplier;
    // 계산된 주문 금액 대비 필요한 증거금을 산출할 때 사용할 레버리지
    private BigDecimal leverage;
    // 실제 거래소에 보낼 주문 타입. MARKET 또는 LIMIT
    private String type;
    // LIMIT 주문일 때 사용할 주문 유지 정책. 예: GTC
    private String timeInForce;
    // 사용자가 직접 지정한 진입 가격. 비우면 현재가를 기준으로 계산
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


    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public void setLimit(String limit) {
        this.limit = limit;
    }

    public void setAtrPeriod(Integer atrPeriod) {
        this.atrPeriod = atrPeriod;
    }

    public void setRiskPercent(BigDecimal riskPercent) {
        this.riskPercent = riskPercent;
    }

    public void setAtrMultiplier(BigDecimal atrMultiplier) {
        this.atrMultiplier = atrMultiplier;
    }

    public void setLeverage(BigDecimal leverage) {
        this.leverage = leverage;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTimeInForce(String timeInForce) {
        this.timeInForce = timeInForce;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
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
