package st.indicator.stindicator.presentation.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.MarketSubscriptionService;
import st.indicator.stindicator.application.service.SessionUser;
import st.indicator.stindicator.presentation.dto.MarketSubscriptionResponseDto;

import java.util.List;

@RestController
public class MarketSubscriptionController implements MarketSubscriptionApi {
    private final MarketSubscriptionService marketSubscriptionService;

    public MarketSubscriptionController(MarketSubscriptionService marketSubscriptionService) {
        this.marketSubscriptionService = marketSubscriptionService;
    }

    @Override
    public List<MarketSubscriptionResponseDto> list(HttpSession session) {
        Object userId = session.getAttribute(SessionUser.USER_ID);
        if (!(userId instanceof Long id)) {
            return List.of();
        }
        return marketSubscriptionService.list(id)
                .stream()
                .map(MarketSubscriptionResponseDto::from)
                .toList();
    }
}
