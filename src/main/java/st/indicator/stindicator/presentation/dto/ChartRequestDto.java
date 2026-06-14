package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "차트 캔들 및 기술 지표 조회 조건")
public class ChartRequestDto {
    @Schema(description = "조회할 USDT 무기한 선물 심볼", example = "BTCUSDT", requiredMode = Schema.RequiredMode.REQUIRED)
    private String symbol;
    @Schema(description = "캔들 주기: 1m, 5m, 15m, 1h, 4h, 1d", example = "1h")
    private String interval;
    @Schema(description = "화면에 반환할 캔들 수. 기본 200, 최대 500", example = "200")
    private Integer limit;
    @Schema(description = "이 시각 이전 캔들을 조회하는 exclusive cursor", example = "1710000000000")
    private Long endTime;
    @Schema(description = "계산할 지표 CSV", example = "SMA,EMA,RSI,MACD")
    private String indicators;
    @Schema(description = "SMA 기간", example = "20")
    private Integer smaPeriod;
    @Schema(description = "EMA 기간", example = "20")
    private Integer emaPeriod;
    @Schema(description = "RSI 기간", example = "14")
    private Integer rsiPeriod;
    @Schema(description = "MACD 단기 EMA 기간", example = "12")
    private Integer macdFastPeriod;
    @Schema(description = "MACD 장기 EMA 기간", example = "26")
    private Integer macdSlowPeriod;
    @Schema(description = "MACD signal EMA 기간", example = "9")
    private Integer macdSignalPeriod;

    public String getSymbol() { return symbol; }
    public String getInterval() { return interval; }
    public Integer getLimit() { return limit; }
    public Long getEndTime() { return endTime; }
    public String getIndicators() { return indicators; }
    public Integer getSmaPeriod() { return smaPeriod; }
    public Integer getEmaPeriod() { return emaPeriod; }
    public Integer getRsiPeriod() { return rsiPeriod; }
    public Integer getMacdFastPeriod() { return macdFastPeriod; }
    public Integer getMacdSlowPeriod() { return macdSlowPeriod; }
    public Integer getMacdSignalPeriod() { return macdSignalPeriod; }
}
