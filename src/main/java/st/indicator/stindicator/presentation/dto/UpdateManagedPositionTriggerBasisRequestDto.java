package st.indicator.stindicator.presentation.dto;

import st.indicator.stindicator.domain.entity.TriggerBasis;

/**
 * 체결 완료 후 관리 포지션의 전략 모드는 유지하면서 SL/TP 평가 기준만 변경한다.
 */
public class UpdateManagedPositionTriggerBasisRequestDto {
    private TriggerBasis stopTriggerBasis;
    private TriggerBasis takeProfitTriggerBasis;

    public TriggerBasis getStopTriggerBasis() {
        return stopTriggerBasis;
    }

    public TriggerBasis getTakeProfitTriggerBasis() {
        return takeProfitTriggerBasis;
    }

    public void setStopTriggerBasis(TriggerBasis stopTriggerBasis) {
        this.stopTriggerBasis = stopTriggerBasis;
    }

    public void setTakeProfitTriggerBasis(TriggerBasis takeProfitTriggerBasis) {
        this.takeProfitTriggerBasis = takeProfitTriggerBasis;
    }
}
