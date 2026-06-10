package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.ManagedPositionCloseReason;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalResponseDto;
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

    @Operation(summary = "종료된 관리 포지션 이력 조회", description = "최근 종료된 포지션부터 조회하며 심볼, 방향, 전략 모드, 청산 사유로 필터링할 수 있습니다.")
    @GetMapping("/managed-positions/history")
    List<ManagedPositionResponseDto> positionHistory(@RequestParam(required = false) String symbol,
                                                     @RequestParam(required = false) String side,
                                                     @RequestParam(required = false) ManagedOrderMode mode,
                                                     @RequestParam(required = false) ManagedPositionCloseReason closeReason);

    @Operation(summary = "매매일지 목록 조회", description = "종료 포지션과 연결된 매매일지를 최근 수정순으로 조회합니다.")
    @GetMapping("/managed-position-journals")
    List<ManagedPositionJournalResponseDto> journals(@RequestParam(required = false) String symbol);

    @Operation(summary = "보유 포지션 상세 조회")
    @GetMapping("/managed-positions/{id}")
    ManagedPositionResponseDto position(@PathVariable Long id);

    @Operation(summary = "포지션 매매일지 조회", description = "관리 포지션 id에 연결된 진입 이유, 복기, 태그를 조회합니다.")
    @GetMapping("/managed-positions/{id}/journal")
    ManagedPositionJournalResponseDto journal(@PathVariable Long id);

    @Operation(summary = "포지션 매매일지 작성/수정", description = "종료된 관리 포지션에 진입 이유와 매매 복기 기록을 저장합니다.")
    @PutMapping("/managed-positions/{id}/journal")
    ManagedPositionJournalResponseDto upsertJournal(@PathVariable Long id,
                                                    @RequestBody ManagedPositionJournalRequestDto request,
                                                    HttpSession session);

    @Operation(summary = "포지션 매매일지 삭제")
    @DeleteMapping("/managed-positions/{id}/journal")
    void deleteJournal(@PathVariable Long id);

    @Operation(summary = "보유 포지션 수동 시장가 청산")
    @PostMapping("/managed-positions/{id}/close")
    ManagedPositionResponseDto closePosition(@PathVariable Long id);
}
