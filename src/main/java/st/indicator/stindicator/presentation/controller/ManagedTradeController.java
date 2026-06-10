package st.indicator.stindicator.presentation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.ManagedTradeService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.domain.entity.ManagedOrderMode;
import st.indicator.stindicator.domain.entity.ManagedPositionCloseReason;
import st.indicator.stindicator.presentation.dto.ManagedAtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalRequestDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionJournalResponseDto;
import st.indicator.stindicator.presentation.dto.ManagedPositionResponseDto;
import st.indicator.stindicator.presentation.dto.PendingOrderResponseDto;
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
    public PendingOrderResponseDto createAtrOrder(ManagedAtrOrderRequestDto request) {
        return PendingOrderResponseDto.from(managedTradeService.createAtrLimitOrder(request));
    }

    @Override
    public List<PendingOrderResponseDto> pendingOrders() {
        return managedTradeService.pendingOrders().stream()
                .map(PendingOrderResponseDto::from)
                .toList();
    }

    @Override
    public PendingOrderResponseDto pendingOrder(Long id) {
        return PendingOrderResponseDto.from(managedTradeService.pendingOrder(id));
    }

    @Override
    public PendingOrderResponseDto cancelPendingOrder(Long id) {
        return PendingOrderResponseDto.from(managedTradeService.cancelPendingOrder(id));
    }

    @Override
    public PendingOrderResponseDto updatePendingConditions(Long id, UpdatePendingOrderConditionsRequestDto request) {
        return PendingOrderResponseDto.from(managedTradeService.updatePendingConditions(id, request));
    }

    @Override
    public List<ManagedPositionResponseDto> positions() {
        return managedTradeService.activePositions().stream()
                .map(ManagedPositionResponseDto::from)
                .toList();
    }

    @Override
    public List<ManagedPositionResponseDto> positionHistory(String symbol, String side, ManagedOrderMode mode,
                                                            ManagedPositionCloseReason closeReason) {
        return managedTradeService.positionHistory(symbol, side, mode, closeReason).stream()
                .map(ManagedPositionResponseDto::from)
                .toList();
    }

    @Override
    public List<ManagedPositionJournalResponseDto> journals(String symbol) {
        return managedTradeService.journals(symbol).stream()
                .map(ManagedPositionJournalResponseDto::from)
                .toList();
    }

    @Override
    public ManagedPositionResponseDto position(Long id) {
        return ManagedPositionResponseDto.from(managedTradeService.position(id));
    }

    @Override
    public ManagedPositionJournalResponseDto journal(Long id) {
        return ManagedPositionJournalResponseDto.from(managedTradeService.journal(id));
    }

    @Override
    public ManagedPositionJournalResponseDto upsertJournal(Long id, ManagedPositionJournalRequestDto request,
                                                           HttpSession session) {
        return ManagedPositionJournalResponseDto.from(
                managedTradeService.upsertJournal(id, sessionUserId(session), request)
        );
    }

    @Override
    public void deleteJournal(Long id) {
        managedTradeService.deleteJournal(id);
    }

    @Override
    public ManagedPositionResponseDto closePosition(Long id) {
        return ManagedPositionResponseDto.from(managedTradeService.closePosition(id));
    }

    private Long sessionUserId(HttpSession session) {
        Object userId = session == null ? null : session.getAttribute(SessionUser.USER_ID);
        if (userId instanceof Long id) {
            return id;
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }
}
