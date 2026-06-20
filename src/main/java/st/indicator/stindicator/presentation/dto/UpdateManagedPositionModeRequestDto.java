package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class UpdateManagedPositionModeRequestDto {
    private ManagedOrderMode mode;
    private TriggerBasis stopTriggerBasis;
    private RaiseStopType stopValueType;
    private BigDecimal stopValue;
    private TriggerBasis takeProfitTriggerBasis;
    private RaiseStopType takeProfitValueType;
    private BigDecimal takeProfitValue;
    private TriggerBasis raiseTriggerBasis;
    private BigDecimal raiseTriggerValue;
    private RaiseStopType raiseStopType;
    private BigDecimal raiseStopValue;

    public ManagedOrderMode getMode() {
        return mode;
    }

    public TriggerBasis getRaiseTriggerBasis() {
        return raiseTriggerBasis;
    }

    public TriggerBasis getStopTriggerBasis() {
        return stopTriggerBasis;
    }

    public RaiseStopType getStopValueType() {
        return stopValueType;
    }

    public BigDecimal getStopValue() {
        return stopValue;
    }

    public TriggerBasis getTakeProfitTriggerBasis() {
        return takeProfitTriggerBasis;
    }

    public RaiseStopType getTakeProfitValueType() {
        return takeProfitValueType;
    }

    public BigDecimal getTakeProfitValue() {
        return takeProfitValue;
    }

    public BigDecimal getRaiseTriggerValue() {
        return raiseTriggerValue;
    }

    public RaiseStopType getRaiseStopType() {
        return raiseStopType;
    }

    public BigDecimal getRaiseStopValue() {
        return raiseStopValue;
    }

    public void setMode(ManagedOrderMode mode) {
        this.mode = mode;
    }

    public void setStopTriggerBasis(TriggerBasis stopTriggerBasis) {
        this.stopTriggerBasis = stopTriggerBasis;
    }

    public void setStopValueType(RaiseStopType stopValueType) {
        this.stopValueType = stopValueType;
    }

    public void setStopValue(BigDecimal stopValue) {
        this.stopValue = stopValue;
    }

    public void setTakeProfitTriggerBasis(TriggerBasis takeProfitTriggerBasis) {
        this.takeProfitTriggerBasis = takeProfitTriggerBasis;
    }

    public void setTakeProfitValueType(RaiseStopType takeProfitValueType) {
        this.takeProfitValueType = takeProfitValueType;
    }

    public void setTakeProfitValue(BigDecimal takeProfitValue) {
        this.takeProfitValue = takeProfitValue;
    }

    public void setRaiseTriggerBasis(TriggerBasis raiseTriggerBasis) {
        this.raiseTriggerBasis = raiseTriggerBasis;
    }

    public void setRaiseTriggerValue(BigDecimal raiseTriggerValue) {
        this.raiseTriggerValue = raiseTriggerValue;
    }

    public void setRaiseStopType(RaiseStopType raiseStopType) {
        this.raiseStopType = raiseStopType;
    }

    public void setRaiseStopValue(BigDecimal raiseStopValue) {
        this.raiseStopValue = raiseStopValue;
    }
}
