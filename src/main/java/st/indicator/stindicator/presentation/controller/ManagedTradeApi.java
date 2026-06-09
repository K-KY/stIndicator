package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionResponseDto;
import st.indicator.stindicator.presentation.dto.PendingOrderResponseDto;
import st.indicator.stindicator.presentation.dto.UpdatePendingOrderConditionsRequestDto;

import java.util.List;

@Tag(name = "Managed Trade", description = "서비스 자체 관리형 TP/SL 주문 및 포지션 API")
public interface ManagedTradeApi {
    @Operation(summary = "관리형 ATR LIMIT 주문 생성", description = "Binance TP/SL 주문 없이 LIMIT 진입 주문과 내부 PendingOrder를 생성합니다.")
    @PostMapping("/managed-orders/atr")
    PendingOrderResponseDto createAtrOrder(@RequestBody ManagedAtrOrderRequestDto request);

    @Operation(summary = "대기중인 주문 목록 조회")
    @GetMapping("/managed-orders/pending")
    List<PendingOrderResponseDto> pendingOrders();

    @Operation(summary = "대기중인 주문 상세 조회")
    @GetMapping("/managed-orders/pending/{id}")
    PendingOrderResponseDto pendingOrder(@PathVariable Long id);

    @Operation(summary = "대기중인 주문 취소", description = "내부 상태와 Binance LIMIT 주문을 함께 취소합니다.")
    @DeleteMapping("/managed-orders/pending/{id}")
    PendingOrderResponseDto cancelPendingOrder(@PathVariable Long id);

    @Operation(summary = "대기중인 주문 TP/SL 조건 수정", description = "체결 전 PendingOrder의 손절가, 익절가, 손익 조건, 손절선 상승 옵션을 수정합니다.")
    @PatchMapping("/managed-orders/pending/{id}/conditions")
    PendingOrderResponseDto updatePendingConditions(@PathVariable Long id,
                                                    @RequestBody UpdatePendingOrderConditionsRequestDto request);

    @Operation(summary = "활성 보유 포지션 목록 조회")
    @GetMapping("/managed-positions")
    List<ManagedPositionResponseDto> positions();

    @Operation(summary = "보유 포지션 상세 조회")
    @GetMapping("/managed-positions/{id}")
    ManagedPositionResponseDto position(@PathVariable Long id);

    @Operation(summary = "보유 포지션 수동 시장가 청산")
    @PostMapping("/managed-positions/{id}/close")
    ManagedPositionResponseDto closePosition(@PathVariable Long id);
}
