package st.indicator.stindicator.presentation.dto;

import java.util.List;
import java.util.Map;

public record ChartResponseDto(
        String symbol,
        String interval,
        List<ChartCandleResponseDto> candles,
        Map<String, Object> indicators,
        boolean hasMore,
        boolean hasOlder,
        boolean hasNewer,
        Long nextBefore,
        Long nextAfter,
        String direction,
        int returnedCount
) {
}
