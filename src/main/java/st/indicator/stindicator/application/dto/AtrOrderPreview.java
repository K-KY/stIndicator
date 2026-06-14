package st.indicator.stindicator.application.dto;

import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class AtrOrderPreview {
    // 미리보기를 계산한 대상 심볼
    private final String symbol;
    // BUY/SELL 기준 방향
    private final String side;
    // ATR 계산에 사용한 캔들 주기
    private final String interval;
    // ATR 계산 기간
    private final Integer atrPeriod;
    // 계산 시점의 가용 자산
    private final BigDecimal availableBalance;
    // 수량 계산의 기준이 된 진입 가격
    private final BigDecimal entryPrice;
    // 해당 심볼/주기 기준으로 계산된 ATR 값
    private final BigDecimal atr;
    // 손절 거리 계산에 사용한 ATR 배수
    private final BigDecimal atrMultiplier;
    // 실제 손절 거리. ATR x atrMultiplier
    private final BigDecimal stopDistance;
    // 요청한 리스크 비율
    private final BigDecimal riskPercent;
    // 이번 주문에 허용한 절대 리스크 금액
    private final BigDecimal riskAmount;
    // 필요 증거금 계산에 사용한 레버리지
    private final BigDecimal leverage;
    // 리스크와 ATR 거리 기준으로 계산된 주문 수량
    private final BigDecimal quantity;
    // quantity x entryPrice 로 계산한 총 주문 금액
    private final BigDecimal notional;
    // 레버리지 적용 후 실제로 필요한 예상 증거금
    private final BigDecimal requiredMargin;
    // 필요 증거금이 가용 잔액을 초과한 금액
    private final BigDecimal shortage;
    // 현재 가용 잔액으로 주문을 생성할 수 있는지 여부
    private final boolean orderable;
    // 방향 기준 손절 예상 가격
    private final BigDecimal stopPrice;
    // 현재 구현 기준 1R 대응 목표 가격
    private final BigDecimal targetPrice;
    // 현재 구현 기준 허용 손실 금액
    private final BigDecimal possibleLoss;
    // 현재 구현 기준 예상 이익 금액
    private final BigDecimal possibleProfit;
    // 손절 트리거 평가 기준
    private final TriggerBasis stopTriggerBasis;
    // 익절 트리거 평가 기준
    private final TriggerBasis takeProfitTriggerBasis;
    // 손절 가격까지 필요한 가격 변동률
    private final BigDecimal priceMovePercentForStop;
    // 익절 가격까지 필요한 가격 변동률
    private final BigDecimal priceMovePercentForTakeProfit;
    // 손절 시 필요 증거금 대비 손실률
    private final BigDecimal marginPnlPercentForStop;
    // 익절 시 필요 증거금 대비 수익률
    private final BigDecimal marginPnlPercentForTakeProfit;

    public AtrOrderPreview(String symbol, String side, String interval, Integer atrPeriod,
                           BigDecimal availableBalance, BigDecimal entryPrice, BigDecimal atr,
                           BigDecimal atrMultiplier, BigDecimal stopDistance, BigDecimal riskPercent,
                           BigDecimal riskAmount, BigDecimal leverage, BigDecimal quantity,
                           BigDecimal notional, BigDecimal requiredMargin, BigDecimal stopPrice,
                           BigDecimal targetPrice, BigDecimal possibleLoss, BigDecimal possibleProfit,
                           TriggerBasis stopTriggerBasis, TriggerBasis takeProfitTriggerBasis,
                           BigDecimal priceMovePercentForStop, BigDecimal priceMovePercentForTakeProfit,
                           BigDecimal marginPnlPercentForStop, BigDecimal marginPnlPercentForTakeProfit) {
        this.symbol = symbol;
        this.side = side;
        this.interval = interval;
        this.atrPeriod = atrPeriod;
        this.availableBalance = availableBalance;
        this.entryPrice = entryPrice;
        this.atr = atr;
        this.atrMultiplier = atrMultiplier;
        this.stopDistance = stopDistance;
        this.riskPercent = riskPercent;
        this.riskAmount = riskAmount;
        this.leverage = leverage;
        this.quantity = quantity;
        this.notional = notional;
        this.requiredMargin = requiredMargin;
        this.shortage = calculateShortage(availableBalance, requiredMargin);
        this.orderable = shortage.signum() == 0;
        this.stopPrice = stopPrice;
        this.targetPrice = targetPrice;
        this.possibleLoss = possibleLoss;
        this.possibleProfit = possibleProfit;
        this.stopTriggerBasis = stopTriggerBasis;
        this.takeProfitTriggerBasis = takeProfitTriggerBasis;
        this.priceMovePercentForStop = priceMovePercentForStop;
        this.priceMovePercentForTakeProfit = priceMovePercentForTakeProfit;
        this.marginPnlPercentForStop = marginPnlPercentForStop;
        this.marginPnlPercentForTakeProfit = marginPnlPercentForTakeProfit;
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

    public Integer getAtrPeriod() {
        return atrPeriod;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public BigDecimal getAtr() {
        return atr;
    }

    public BigDecimal getAtrMultiplier() {
        return atrMultiplier;
    }

    public BigDecimal getStopDistance() {
        return stopDistance;
    }

    public BigDecimal getRiskPercent() {
        return riskPercent;
    }

    public BigDecimal getRiskAmount() {
        return riskAmount;
    }

    public BigDecimal getLeverage() {
        return leverage;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getNotional() {
        return notional;
    }

    public BigDecimal getRequiredMargin() {
        return requiredMargin;
    }

    public BigDecimal getShortage() {
        return shortage;
    }

    public boolean isOrderable() {
        return orderable;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }

    public BigDecimal getTargetPrice() {
        return targetPrice;
    }

    public BigDecimal getPossibleLoss() {
        return possibleLoss;
    }

    public BigDecimal getPossibleProfit() {
        return possibleProfit;
    }

    public TriggerBasis getStopTriggerBasis() {
        return stopTriggerBasis;
    }

    public TriggerBasis getTakeProfitTriggerBasis() {
        return takeProfitTriggerBasis;
    }

    public BigDecimal getPriceMovePercentForStop() {
        return priceMovePercentForStop;
    }

    public BigDecimal getPriceMovePercentForTakeProfit() {
        return priceMovePercentForTakeProfit;
    }

    public BigDecimal getMarginPnlPercentForStop() {
        return marginPnlPercentForStop;
    }

    public BigDecimal getMarginPnlPercentForTakeProfit() {
        return marginPnlPercentForTakeProfit;
    }

    private BigDecimal calculateShortage(BigDecimal available, BigDecimal required) {
        if (available == null || required == null) {
            return BigDecimal.ZERO;
        }
        return required.subtract(available).max(BigDecimal.ZERO);
    }
}
