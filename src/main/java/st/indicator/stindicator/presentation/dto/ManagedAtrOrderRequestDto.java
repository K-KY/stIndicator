package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class ManagedAtrOrderRequestDto {
    private String symbol;
    private String side;
    private String interval;
    private String limit;
    private Integer atrPeriod;
    private BigDecimal riskPercent;
    private BigDecimal atrMultiplier;
    private BigDecimal leverage;
    private TriggerBasis stopTriggerBasis = TriggerBasis.PRICE_PERCENT;
    private TriggerBasis takeProfitTriggerBasis = TriggerBasis.PRICE_PERCENT;
    private String type;
    private String timeInForce;
    private BigDecimal entryPrice;
    private ManagedOrderMode mode;
    private boolean raiseStopEnabled;
    private RaiseStopType raiseTriggerType;
    private BigDecimal raiseTriggerValue;
    private RaiseStopType raiseStopType;
    private BigDecimal raiseStopValue;

    public String getSymbol() { return symbol; }
    public String getSide() { return side; }
    public String getInterval() { return interval; }
    public String getLimit() { return limit; }
    public Integer getAtrPeriod() { return atrPeriod; }
    public BigDecimal getRiskPercent() { return riskPercent; }
    public BigDecimal getAtrMultiplier() { return atrMultiplier; }
    public BigDecimal getLeverage() { return leverage; }
    public TriggerBasis getStopTriggerBasis() { return stopTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : stopTriggerBasis; }
    public TriggerBasis getTakeProfitTriggerBasis() { return takeProfitTriggerBasis == null ? TriggerBasis.PRICE_PERCENT : takeProfitTriggerBasis; }
    public String getType() { return type; }
    public String getTimeInForce() { return timeInForce; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public ManagedOrderMode getMode() { return mode; }
    public boolean isRaiseStopEnabled() { return raiseStopEnabled; }
    public RaiseStopType getRaiseTriggerType() { return raiseTriggerType; }
    public BigDecimal getRaiseTriggerValue() { return raiseTriggerValue; }
    public RaiseStopType getRaiseStopType() { return raiseStopType; }
    public BigDecimal getRaiseStopValue() { return raiseStopValue; }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setSide(String side) { this.side = side; }
    public void setInterval(String interval) { this.interval = interval; }
    public void setLimit(String limit) { this.limit = limit; }
    public void setAtrPeriod(Integer atrPeriod) { this.atrPeriod = atrPeriod; }
    public void setRiskPercent(BigDecimal riskPercent) { this.riskPercent = riskPercent; }
    public void setAtrMultiplier(BigDecimal atrMultiplier) { this.atrMultiplier = atrMultiplier; }
    public void setLeverage(BigDecimal leverage) { this.leverage = leverage; }
    public void setStopTriggerBasis(TriggerBasis stopTriggerBasis) { this.stopTriggerBasis = stopTriggerBasis; }
    public void setTakeProfitTriggerBasis(TriggerBasis takeProfitTriggerBasis) { this.takeProfitTriggerBasis = takeProfitTriggerBasis; }
    public void setType(String type) { this.type = type; }
    public void setTimeInForce(String timeInForce) { this.timeInForce = timeInForce; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public void setMode(ManagedOrderMode mode) { this.mode = mode; }
    public void setRaiseStopEnabled(boolean raiseStopEnabled) { this.raiseStopEnabled = raiseStopEnabled; }
    public void setRaiseTriggerType(RaiseStopType raiseTriggerType) { this.raiseTriggerType = raiseTriggerType; }
    public void setRaiseTriggerValue(BigDecimal raiseTriggerValue) { this.raiseTriggerValue = raiseTriggerValue; }
    public void setRaiseStopType(RaiseStopType raiseStopType) { this.raiseStopType = raiseStopType; }
    public void setRaiseStopValue(BigDecimal raiseStopValue) { this.raiseStopValue = raiseStopValue; }
}
