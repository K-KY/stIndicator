package st.indicator.stindicator.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import st.indicator.stindicator.presentation.dto.MarketSubscriptionResponseDto;

import java.util.List;

@Tag(name = "Market Subscription", description = "로그인 사용자의 실시간 시장 데이터 구독 API")
@RequestMapping("/api/market-subscriptions")
public interface MarketSubscriptionApi {
    @GetMapping
    @Operation(summary = "내 시장 구독 목록 조회", description = "세션 사용자 기준으로 저장된 시장 가격 구독 심볼을 조회한다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "구독 목록 조회 성공")})
    List<MarketSubscriptionResponseDto> list(HttpSession session);
}
