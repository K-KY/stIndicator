package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.RaiseStopType;
import st.indicator.stindicator.domain.entity.TriggerBasis;

import java.math.BigDecimal;

public class AddRaisingStopRequestDto {
    private TriggerBasis raiseTriggerBasis;
    private BigDecimal raiseTriggerValue;
    private RaiseStopType raiseStopType;
    private BigDecimal raiseStopValue;

    public TriggerBasis getRaiseTriggerBasis() {
        return raiseTriggerBasis;
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
