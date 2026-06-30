package st.indicator.stindicator.presentation.dto;

import java.math.BigDecimal;
import java.util.List;

public class SymbolMonitorDto {
    // 구독 또는 해제 같은 모니터링 요청 타입
    private String type;
    // 실시간으로 추적할 심볼 목록
    private List<String> symbols;
    // 심볼별로 모니터링할 캔들 주기
    private String interval;
    // 차트 구독 응답을 구분하는 프론트 생성 ID
    private String subscriptionId;
    // 지표 설정 변경 시 이전 이벤트를 폐기하기 위한 버전
    private Long configVersion;
    // 실시간 계산할 EMA 기간 목록
    private List<Integer> emaPeriods;
    // 실시간 계산할 SMA 기간 목록
    private List<Integer> smaPeriods;
    // 실시간 계산할 볼린저 밴드 기간
    private Integer bollingerPeriod;
    // 실시간 계산할 볼린저 밴드 표준편차 배수
    private BigDecimal bollingerDeviation;
    // 실시간 VWAP 계산 여부
    private Boolean vwap;

    public SymbolMonitorDto() {
    }

    public SymbolMonitorDto(String type, List<String> symbols, String interval) {
        this.type = type;
        this.symbols = symbols;
        this.interval = interval;
    }

    public String getType() {
        return type;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public String getInterval() {
        return interval;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public Long getConfigVersion() {
        return configVersion;
    }

    public List<Integer> getEmaPeriods() {
        return emaPeriods;
    }

    public List<Integer> getSmaPeriods() {
        return smaPeriods;
    }

    public Integer getBollingerPeriod() {
        return bollingerPeriod;
    }

    public BigDecimal getBollingerDeviation() {
        return bollingerDeviation;
    }

    public Boolean getVwap() {
        return vwap;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public void setInterval(String interval) {
        this.interval = interval;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public void setConfigVersion(Long configVersion) {
        this.configVersion = configVersion;
    }

    public void setEmaPeriods(List<Integer> emaPeriods) {
        this.emaPeriods = emaPeriods;
    }

    public void setSmaPeriods(List<Integer> smaPeriods) {
        this.smaPeriods = smaPeriods;
    }

    public void setBollingerPeriod(Integer bollingerPeriod) {
        this.bollingerPeriod = bollingerPeriod;
    }

    public void setBollingerDeviation(BigDecimal bollingerDeviation) {
        this.bollingerDeviation = bollingerDeviation;
    }

    public void setVwap(Boolean vwap) {
        this.vwap = vwap;
    }
}
