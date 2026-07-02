package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

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
    @Schema(description = "이 openTime 이전의 과거 캔들을 조회하는 exclusive cursor. endTime과 같은 의미로 호환된다.", example = "1710000000000")
    private Long before;
    @Schema(description = "이 openTime 이후의 최신 방향 캔들을 조회하는 exclusive cursor", example = "1710000000000")
    private Long after;
    @Schema(description = "계산할 지표 CSV", example = "SMA,EMA,RSI,MACD")
    private String indicators;
    @Schema(description = "계산할 EMA 기간 CSV. 예: 20,60,120", example = "20,60")
    private String emaPeriods;
    @Schema(description = "계산할 SMA 기간 CSV. 예: 20,120", example = "20,120")
    private String smaPeriods;
    @Schema(description = "볼린저 밴드 기간. 값이 있고 bollingerEnabled=true이면 계산한다.", example = "20")
    private Integer bollingerPeriod;
    @Schema(description = "볼린저 밴드 표준편차 배수", example = "2")
    private BigDecimal bollingerDeviation;
    @Schema(description = "VWAP 계산 여부", example = "true")
    private Boolean vwap;
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
    @Schema(description = "ADX/DMI DI 기간", example = "14")
    private Integer adxDiPeriod;
    @Schema(description = "ADX smoothing 기간", example = "14")
    private Integer adxSmoothingPeriod;

    public String getSymbol() { return symbol; }
    public String getInterval() { return interval; }
    public Integer getLimit() { return limit; }
    public Long getEndTime() { return endTime; }
    public Long getBefore() { return before; }
    public Long getAfter() { return after; }
    public String getIndicators() { return indicators; }
    public String getEmaPeriods() { return emaPeriods; }
    public String getSmaPeriods() { return smaPeriods; }
    public Integer getBollingerPeriod() { return bollingerPeriod; }
    public BigDecimal getBollingerDeviation() { return bollingerDeviation; }
    public Boolean getVwap() { return vwap; }
    public Integer getSmaPeriod() { return smaPeriod; }
    public Integer getEmaPeriod() { return emaPeriod; }
    public Integer getRsiPeriod() { return rsiPeriod; }
    public Integer getMacdFastPeriod() { return macdFastPeriod; }
    public Integer getMacdSlowPeriod() { return macdSlowPeriod; }
    public Integer getMacdSignalPeriod() { return macdSignalPeriod; }
    public Integer getAdxDiPeriod() { return adxDiPeriod; }
    public Integer getAdxSmoothingPeriod() { return adxSmoothingPeriod; }

    public void setSymbol(String symbol) { this.symbol = symbol; }
    public void setInterval(String interval) { this.interval = interval; }
    public void setLimit(Integer limit) { this.limit = limit; }
    public void setEndTime(Long endTime) { this.endTime = endTime; }
    public void setBefore(Long before) { this.before = before; }
    public void setAfter(Long after) { this.after = after; }
    public void setIndicators(String indicators) { this.indicators = indicators; }
    public void setEmaPeriods(String emaPeriods) { this.emaPeriods = emaPeriods; }
    public void setSmaPeriods(String smaPeriods) { this.smaPeriods = smaPeriods; }
    public void setBollingerPeriod(Integer bollingerPeriod) { this.bollingerPeriod = bollingerPeriod; }
    public void setBollingerDeviation(BigDecimal bollingerDeviation) { this.bollingerDeviation = bollingerDeviation; }
    public void setVwap(Boolean vwap) { this.vwap = vwap; }
    public void setSmaPeriod(Integer smaPeriod) { this.smaPeriod = smaPeriod; }
    public void setEmaPeriod(Integer emaPeriod) { this.emaPeriod = emaPeriod; }
    public void setRsiPeriod(Integer rsiPeriod) { this.rsiPeriod = rsiPeriod; }
    public void setMacdFastPeriod(Integer macdFastPeriod) { this.macdFastPeriod = macdFastPeriod; }
    public void setMacdSlowPeriod(Integer macdSlowPeriod) { this.macdSlowPeriod = macdSlowPeriod; }
    public void setMacdSignalPeriod(Integer macdSignalPeriod) { this.macdSignalPeriod = macdSignalPeriod; }
    public void setAdxDiPeriod(Integer adxDiPeriod) { this.adxDiPeriod = adxDiPeriod; }
    public void setAdxSmoothingPeriod(Integer adxSmoothingPeriod) { this.adxSmoothingPeriod = adxSmoothingPeriod; }
}
