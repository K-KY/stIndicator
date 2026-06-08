package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import st.indicator.stindicator.presentation.dto.WebSocketStatusResponseDto;

@Tag(name = "WebSocket Status", description = "클라이언트 및 Binance WebSocket 연결 상태 API")
@RequestMapping("/api/ws")
public interface WebSocketStatusApi {
    @GetMapping("/status")
    @Operation(summary = "WebSocket 연결 상태 조회", description = "서버에 연결된 클라이언트 소켓 수와 Binance 상위 소켓 수를 조회한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "WebSocket 상태 조회 성공")})
    WebSocketStatusResponseDto status();
}
