package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class UpdatePendingOrderConditionsRequestDto {
    private BigDecimal stopPrice;
    private BigDecimal targetPrice;
    private BigDecimal possibleLoss;
    private BigDecimal possibleProfit;
    private TriggerBasis stopTriggerBasis;
    private TriggerBasis takeProfitTriggerBasis;
    private RaiseStopType raiseTriggerType;
    private BigDecimal raiseTriggerValue;
    private RaiseStopType raiseStopType;
    private BigDecimal raiseStopValue;

    public BigDecimal getStopPrice() { return stopPrice; }
    public BigDecimal getTargetPrice() { return targetPrice; }
    public BigDecimal getPossibleLoss() { return possibleLoss; }
    public BigDecimal getPossibleProfit() { return possibleProfit; }
    public TriggerBasis getStopTriggerBasis() { return stopTriggerBasis; }
    public TriggerBasis getTakeProfitTriggerBasis() { return takeProfitTriggerBasis; }
    public RaiseStopType getRaiseTriggerType() { return raiseTriggerType; }
    public BigDecimal getRaiseTriggerValue() { return raiseTriggerValue; }
    public RaiseStopType getRaiseStopType() { return raiseStopType; }
    public BigDecimal getRaiseStopValue() { return raiseStopValue; }

    public void setStopPrice(BigDecimal stopPrice) { this.stopPrice = stopPrice; }
    public void setTargetPrice(BigDecimal targetPrice) { this.targetPrice = targetPrice; }
    public void setPossibleLoss(BigDecimal possibleLoss) { this.possibleLoss = possibleLoss; }
    public void setPossibleProfit(BigDecimal possibleProfit) { this.possibleProfit = possibleProfit; }
    public void setStopTriggerBasis(TriggerBasis stopTriggerBasis) { this.stopTriggerBasis = stopTriggerBasis; }
    public void setTakeProfitTriggerBasis(TriggerBasis takeProfitTriggerBasis) { this.takeProfitTriggerBasis = takeProfitTriggerBasis; }
    public void setRaiseTriggerType(RaiseStopType raiseTriggerType) { this.raiseTriggerType = raiseTriggerType; }
    public void setRaiseTriggerValue(BigDecimal raiseTriggerValue) { this.raiseTriggerValue = raiseTriggerValue; }
    public void setRaiseStopType(RaiseStopType raiseStopType) { this.raiseStopType = raiseStopType; }
    public void setRaiseStopValue(BigDecimal raiseStopValue) { this.raiseStopValue = raiseStopValue; }
}
