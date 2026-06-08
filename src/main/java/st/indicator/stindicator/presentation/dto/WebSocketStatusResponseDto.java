package st.indicator.stindicator.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "WebSocket 연결 상태 응답")
public class WebSocketStatusResponseDto {
    @Schema(description = "현재 서버에 연결된 클라이언트 WebSocket 세션 수", example = "2")
    private final int clientSocketCount;

    @Schema(description = "서버가 Binance와 유지 중인 상위 WebSocket 연결 수", example = "1")
    private final int binanceSocketCount;

    @Schema(description = "Binance 상위 연결에 등록된 stream 구독 수", example = "6")
    private final int binanceSubscriptionCount;

    public WebSocketStatusResponseDto(int clientSocketCount, int binanceSocketCount, int binanceSubscriptionCount) {
        this.clientSocketCount = clientSocketCount;
        this.binanceSocketCount = binanceSocketCount;
        this.binanceSubscriptionCount = binanceSubscriptionCount;
    }

    public int getClientSocketCount() {
        return clientSocketCount;
    }

    public int getBinanceSocketCount() {
        return binanceSocketCount;
    }

    public int getBinanceSubscriptionCount() {
        return binanceSubscriptionCount;
    }
}
