package st.indicator.stindicator.domain.utils.candle;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * 바이낸스 API를 기준으로 매핑함<br>
 */
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class Candle {

    @JsonProperty(index = 0)
    private Long openTime;

    @JsonProperty(index = 1)
    private BigDecimal open;

    @JsonProperty(index = 2)
    private BigDecimal high;

    @JsonProperty(index = 3)
    private BigDecimal low;

    @JsonProperty(index = 4)
    private BigDecimal close;

    @JsonProperty(index = 5)
    private BigDecimal volume;

    @JsonProperty(index = 6)
    private Long closeTime;

    @JsonProperty(index = 7)
    private String quoteAssetVolume;

    @JsonProperty(index = 8)
    private Integer numberOfTrades;

    @JsonProperty(index = 9)
    private String takerBuyBaseAssetVolume;

    @JsonProperty(index = 10)
    private String takerBuyQuoteAssetVolume;

    @JsonProperty(index = 11)
    private String ignore;

    public Candle() {
    }

    public Candle(Long openTime, BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close,
                  BigDecimal volume, Long closeTime) {
        this.openTime = openTime;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.closeTime = closeTime;
    }

    public Long getOpenTime() {
        return openTime;
    }

    public BigDecimal getOpen() {
        return open;
    }

    public BigDecimal getHigh() {
        return high;
    }

    public BigDecimal getLow() {
        return low;
    }

    public BigDecimal getClose() {
        return close;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public Long getCloseTime() {
        return closeTime;
    }

    @Override
    public String toString() {
        return "Candle{" +
                "openTime=" + openTime +
                ", open='" + open + '\'' +
                ", high='" + high + '\'' +
                ", low='" + low + '\'' +
                ", close='" + close + '\'' +
                ", volume='" + volume + '\'' +
                ", closeTime=" + closeTime +
                '}';
    }
}
//[
//        [
//        1499040000000,      // Open time
//        "0.01634790",       // Open
//        "0.80000000",       // High
//        "0.01575800",       // Low
//        "0.01577100",       // Close
//        "148976.11427815",  // Volume
//        1499644799999,      // Close time
//        "2434.19055334",    // Quote asset volume
//        308,                // Number of trades
//        "1756.87402397",    // Taker buy base asset volume
//        "28.46694368",      // Taker buy quote asset volume
//        "17928899.62484339" // Ignore.
//        ]
//        ]
