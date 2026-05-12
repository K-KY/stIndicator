package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.application.dto.AtrOrderCommand;

import java.math.BigDecimal;

public class AtrOrderRequestDto {
    private String symbol;//이름
    private String side;//매수 혹은 판매
    private String interval;//캔들 간격
    private String limit;//ATR계산 캔들 간격 150 -> 150개의 캔들로 ATR 계산
    private Integer atrPeriod;//TR의 범위 14-> 캔들 중 14개의 평균을 냄
    private BigDecimal riskPercent;//보유 자산 대비 최대 리스크
    private BigDecimal atrMultiplier;//포지션 진입 ATR 배수
    private BigDecimal leverage;//레버리지 비율
    private String type;//LIMIT 만 사용
    private String timeInForce;//뭔지 모름 GTC 사용중
    private BigDecimal entryPrice;//진입 가격

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
