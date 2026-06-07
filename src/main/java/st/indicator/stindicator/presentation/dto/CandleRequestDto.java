package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import st.indicator.stindicator.application.dto.CandleCommand;

@Schema(description = "캔들/ATR 조회에 사용하는 공통 조회 조건")
public class CandleRequestDto {
    @Schema(description = "조회할 거래 심볼", example = "BTCUSDT")
    String symbol;
    @Schema(description = "조회할 캔들 주기", example = "1h")
    String interval;
    @Schema(description = "조회할 캔들 개수", example = "150")
    String limit;

    public CandleRequestDto(String symbol, String interval, String limit) {
        this.symbol = symbol;
        this.interval = interval;
        this.limit = limit;
    }

    public CandleRequestDto() {
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public String getLimit() {
        return limit;
    }

    public CandleCommand toCommand() {
        return new CandleCommand(symbol, interval, limit);
    }
}
