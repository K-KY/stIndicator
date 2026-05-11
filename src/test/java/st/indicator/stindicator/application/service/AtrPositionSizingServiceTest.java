package st.indicator.stindicator.application.service;

import com.java.candle.Candle;
import org.junit.jupiter.api.Test;
import st.indicator.stindicator.application.dto.AtrOrderPreview;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtrPositionSizingServiceTest {

    private final AtrPositionSizingService service = new AtrPositionSizingService();

    @Test
    void calculateAtrAndPreviewByRiskPercent() throws Exception {
        BigDecimal atr = service.calculateAtr(candles(), 3);
        AtrOrderPreview preview = service.preview(
                "BTCUSDT",
                "BUY",
                "1h",
                3,
                new BigDecimal("1000"),
                new BigDecimal("100"),
                atr,
                new BigDecimal("1"),
                new BigDecimal("1"),
                new BigDecimal("10")
        );

        assertTrue(atr.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(new BigDecimal("10.00000000"), preview.getRiskAmount());
        assertEquals(new BigDecimal("2.50000000"), preview.getQuantity());
        assertEquals(new BigDecimal("25.00000000"), preview.getRequiredMargin());
    }

    private List<Candle> candles() throws Exception {
        List<Candle> candles = new ArrayList<>();
        candles.add(candle("100", "103", "99", "102"));
        candles.add(candle("102", "105", "101", "104"));
        candles.add(candle("104", "106", "102", "103"));
        candles.add(candle("103", "107", "103", "105"));
        candles.add(candle("105", "108", "104", "106"));
        return candles;
    }

    private Candle candle(String open, String high, String low, String close) throws Exception {
        Candle candle = new Candle();
        set(candle, "open", new BigDecimal(open));
        set(candle, "high", new BigDecimal(high));
        set(candle, "low", new BigDecimal(low));
        set(candle, "close", new BigDecimal(close));
        return candle;
    }

    private void set(Candle candle, String fieldName, BigDecimal value) throws Exception {
        Field field = Candle.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(candle, value);
    }
}
