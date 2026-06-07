package st.indicator.stindicator.presentation.ws.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import st.indicator.stindicator.presentation.ws.dto.LimitOrderRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MarketOrderRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MonitorStartRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MonitorStopRequestDto;
import st.indicator.stindicator.presentation.ws.dto.OrderExecutionResponseDto;
import st.indicator.stindicator.presentation.ws.dto.PositionMonitorResponseDto;

import java.util.List;

@Tag(name = "Monitor", description = "포지션 모니터링, 트레일링 스탑, 수동 주문 API")
@RequestMapping("/api")
public interface MonitorApi {

    @PostMapping("/monitor/start")
    @Operation(summary = "모니터링 시작", description = "포지션 추적과 트레일링 스탑 계산을 시작한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모니터링 시작 성공")
    })
    PositionMonitorResponseDto start(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "모니터링 시작 요청", required = true) MonitorStartRequestDto request);

    @PostMapping("/monitor/stop")
    @Operation(summary = "모니터링 중지", description = "지정한 모니터링 항목을 중지한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모니터링 중지 성공")
    })
    PositionMonitorResponseDto stop(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "모니터링 중지 요청", required = true) MonitorStopRequestDto request);

    @GetMapping("/monitor/list")
    @Operation(summary = "모니터링 목록 조회", description = "특정 사용자의 활성/중지 모니터링 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "모니터링 목록 조회 성공")
    })
    List<PositionMonitorResponseDto> list(@RequestParam @Parameter(description = "사용자 ID") Long userId);

    @PostMapping("/order/market")
    @Operation(summary = "시장가 주문", description = "사용자 지정 수량으로 시장가 주문을 실행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "시장가 주문 성공")
    })
    OrderExecutionResponseDto marketOrder(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "시장가 주문 요청", required = true) MarketOrderRequestDto request);

    @PostMapping("/order/limit")
    @Operation(summary = "지정가 주문", description = "사용자 지정 수량과 가격으로 지정가 주문을 실행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "지정가 주문 성공")
    })
    OrderExecutionResponseDto limitOrder(@RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "지정가 주문 요청", required = true) LimitOrderRequestDto request);
}
