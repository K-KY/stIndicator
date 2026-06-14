package st.indicator.stindicator.presentation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import st.indicator.stindicator.application.service.ManagedTradeService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.ManagedPositionCloseReason;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalResponseDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionResponseDto;
import st.indicator.stindicator.presentation.dto.ManagedStopHistoryResponseDto;
import st.indicator.stindicator.presentation.dto.PendingOrderResponseDto;
import st.indicator.stindicator.presentation.dto.UpdateManagedPositionTriggerBasisRequestDto;
import st.indicator.stindicator.presentation.dto.UpdatePendingOrderConditionsRequestDto;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ManagedTradeController implements ManagedTradeApi {
    private final ManagedTradeService managedTradeService;

    public ManagedTradeController(ManagedTradeService managedTradeService) {
        this.managedTradeService = managedTradeService;
    }

    @Override
    public PendingOrderResponseDto createAtrOrder(ManagedAtrOrderRequestDto request, HttpSession session) {
        return PendingOrderResponseDto.from(managedTradeService.createAtrLimitOrder(requireSessionUserId(session), request));
    }

    @Override
    public List<PendingOrderResponseDto> pendingOrders(HttpSession session) {
        return managedTradeService.pendingOrders(requireSessionUserId(session)).stream()
                .map(PendingOrderResponseDto::from)
                .toList();
    }

    @Override
    public PendingOrderResponseDto pendingOrder(Long id, HttpSession session) {
        return PendingOrderResponseDto.from(managedTradeService.pendingOrder(requireSessionUserId(session), id));
    }

    @Override
    public PendingOrderResponseDto cancelPendingOrder(Long id, HttpSession session) {
        return PendingOrderResponseDto.from(managedTradeService.cancelPendingOrder(requireSessionUserId(session), id));
    }

    @Override
    public PendingOrderResponseDto updatePendingConditions(Long id, UpdatePendingOrderConditionsRequestDto request,
                                                           HttpSession session) {
        return PendingOrderResponseDto.from(managedTradeService.updatePendingConditions(requireSessionUserId(session), id, request));
    }

    @Override
    public List<ManagedPositionResponseDto> positions(HttpSession session) {
        return managedTradeService.activePositions(requireSessionUserId(session)).stream()
                .map(ManagedPositionResponseDto::from)
                .toList();
    }

    @Override
    public List<ManagedPositionResponseDto> positionHistory(String symbol, String side, ManagedOrderMode mode,
                                                            ManagedPositionCloseReason closeReason,
                                                            HttpSession session) {
        return managedTradeService.positionHistory(requireSessionUserId(session), symbol, side, mode, closeReason).stream()
                .map(ManagedPositionResponseDto::from)
                .toList();
    }

    @Override
    public List<ManagedPositionJournalResponseDto> journals(String symbol, HttpSession session) {
        return managedTradeService.journals(requireSessionUserId(session), symbol).stream()
                .map(ManagedPositionJournalResponseDto::from)
                .toList();
    }

    @Override
    public ManagedPositionResponseDto position(Long id, HttpSession session) {
        return ManagedPositionResponseDto.from(managedTradeService.position(requireSessionUserId(session), id));
    }

    @Override
    public ManagedPositionResponseDto updatePositionTriggerBasis(
            Long id,
            UpdateManagedPositionTriggerBasisRequestDto request,
            HttpSession session
    ) {
        return ManagedPositionResponseDto.from(
                managedTradeService.updatePositionTriggerBasis(requireSessionUserId(session), id, request)
        );
    }

    @Override
    public List<ManagedStopHistoryResponseDto> stopHistory(Long id, HttpSession session) {
        return managedTradeService.stopHistory(requireSessionUserId(session), id).stream()
                .map(ManagedStopHistoryResponseDto::from)
                .toList();
    }

    @Override
    public ManagedPositionJournalResponseDto journal(Long id, HttpSession session) {
        return ManagedPositionJournalResponseDto.from(managedTradeService.journal(requireSessionUserId(session), id));
    }

    @Override
    public ManagedPositionJournalResponseDto upsertJournal(Long id, ManagedPositionJournalRequestDto request,
                                                           HttpSession session) {
        return ManagedPositionJournalResponseDto.from(
                managedTradeService.upsertJournal(id, requireSessionUserId(session), request)
        );
    }

    @Override
    public void deleteJournal(Long id, HttpSession session) {
        managedTradeService.deleteJournal(requireSessionUserId(session), id);
    }

    @Override
    public ManagedPositionResponseDto closePosition(Long id, HttpSession session) {
        return ManagedPositionResponseDto.from(managedTradeService.closePosition(requireSessionUserId(session), id));
    }

    private Long requireSessionUserId(HttpSession session) {
        Object userId = session == null ? null : session.getAttribute(SessionUser.USER_ID);
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
}
