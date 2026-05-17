package st.indicator.stindicator.presentation.dto;

import java.util.List;

public class SymbolMonitorDto {
    // 구독 또는 해제 같은 모니터링 요청 타입
    private String type;
    // 실시간으로 추적할 심볼 목록
    private List<String> symbols;
    // 심볼별로 모니터링할 캔들 주기
    private String interval;

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
}
