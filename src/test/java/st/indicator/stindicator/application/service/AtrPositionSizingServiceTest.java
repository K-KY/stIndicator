package st.indicator.stindicator.application.service;

import org.junit.jupiter.api.Test;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.domain.utils.candle.Candle;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void previewUsesShortDirectionForSell() {
        AtrOrderPreview preview = service.preview(
                "ETHUSDT",
                "SELL",
                "4h",
                14,
                new BigDecimal("500"),
                new BigDecimal("100"),
                new BigDecimal("5"),
                new BigDecimal("2"),
                new BigDecimal("1.5"),
                new BigDecimal("5")
        );

        assertEquals(new BigDecimal("10.00000000"), preview.getRiskAmount());
        assertEquals(new BigDecimal("1.33333333"), preview.getQuantity());
        assertEquals(new BigDecimal("26.66666660"), preview.getRequiredMargin());
        assertEquals(new BigDecimal("107.5"), preview.getStopPrice());
        assertEquals(new BigDecimal("92.5"), preview.getTargetPrice());
    }

    @Test
    void calculateAtrFailsWhenCandleCountIsInsufficient() {
        assertThrows(IllegalArgumentException.class, () -> service.calculateAtr(new ArrayList<>(), 14));
    }

    @Test
    void previewMarksOrderBlockedWhenRequiredMarginExceedsAvailableBalance() {
        AtrOrderPreview preview = service.preview(
                "BTCUSDT",
                "BUY",
                "4h",
                14,
                new BigDecimal("100"),
                new BigDecimal("60000"),
                new BigDecimal("100"),
                new BigDecimal("10"),
                BigDecimal.ONE,
                BigDecimal.ONE
        );

        assertTrue(preview.getRequiredMargin().compareTo(preview.getAvailableBalance()) > 0);
        assertEquals(preview.getRequiredMargin().subtract(preview.getAvailableBalance()), preview.getShortage());
        assertEquals(false, preview.isOrderable());
    }

    @Test
    void previewMarksOrderableWhenAvailableBalanceCoversMargin() {
        AtrOrderPreview preview = service.preview(
                "BTCUSDT",
                "BUY",
                "4h",
                14,
                new BigDecimal("1000"),
                new BigDecimal("100"),
                new BigDecimal("10"),
                BigDecimal.ONE,
                BigDecimal.ONE,
                new BigDecimal("5")
        );

        assertEquals(BigDecimal.ZERO, preview.getShortage());
        assertTrue(preview.isOrderable());
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
