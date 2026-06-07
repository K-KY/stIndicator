package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.application.dto.AtrOrderCommand;

import java.math.BigDecimal;

@Schema(description = "ATR 기반 주문 계산 및 실행 요청 DTO")
public class AtrOrderRequestDto {
    @Schema(description = "ATR 기준 주문을 계산하고 실행할 대상 심볼", example = "BTCUSDT")
    private String symbol;
    @Schema(description = "주문 방향", example = "BUY")
    private String side;
    @Schema(description = "ATR 계산에 사용할 캔들 주기", example = "1h")
    private String interval;
    @Schema(description = "ATR 계산용 캔들 조회 개수", example = "150")
    private String limit;
    @Schema(description = "ATR 기간", example = "14")
    private Integer atrPeriod;
    @Schema(description = "가용 자산 대비 허용 리스크 비율", example = "1")
    private BigDecimal riskPercent;
    @Schema(description = "손절 거리 계산에 사용할 ATR 배수", example = "1")
    private BigDecimal atrMultiplier;
    @Schema(description = "계산된 주문 금액 대비 필요한 증거금 산출에 사용할 레버리지", example = "10")
    private BigDecimal leverage;
    @Schema(description = "실제 거래소에 보낼 주문 타입", example = "MARKET")
    private String type;
    @Schema(description = "LIMIT 주문일 때 사용할 주문 유지 정책", example = "GTC")
    private String timeInForce;
    @Schema(description = "사용자가 직접 지정한 진입 가격", example = "60000")
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
