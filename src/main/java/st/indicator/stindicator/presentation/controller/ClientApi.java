package st.indicator.stindicator.presentation.controller;

import com.java.candle.Candle;
import jakarta.servlet.http.HttpSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.domain.entity.AssetBalance;
import st.indicator.stindicator.domain.entity.ExchangeSymbol;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionRisk;
import st.indicator.stindicator.domain.entity.SymbolPrice;
import st.indicator.stindicator.domain.entity.UserOrder;
import st.indicator.stindicator.presentation.dto.AtrOrderRequestDto;
import st.indicator.stindicator.presentation.dto.CandleRequestDto;
import st.indicator.stindicator.presentation.dto.OrderRequestDto;

import java.math.BigDecimal;
import java.util.List;

@Tag(name = "Client", description = "ATR 주문, 시세 조회, 자산/포지션/주문 관리 API")
@RequestMapping("/client")
public interface ClientApi {

    @GetMapping("/candles")
    @Operation(summary = "과거 캔들 조회", description = "지정한 심볼과 주기로 과거 캔들 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캔들 조회 성공")
    })
    List<Candle> getCandles(@ParameterObject CandleRequestDto dto);

    @GetMapping("/balances")
    @Operation(summary = "총 지갑 잔고 조회", description = "선물 계정의 총 지갑 잔고를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "잔고 조회 성공")
    })
    BigDecimal getBalance();

    @GetMapping("/atrs")
    @Operation(summary = "ATR 조회", description = "지정한 심볼과 주기로 ATR 값을 계산해 반환한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ATR 조회 성공")
    })
    BigDecimal getAtr(@ParameterObject CandleRequestDto dto);

    @GetMapping("/assets")
    @Operation(summary = "보유 자산 목록 조회", description = "선물 계정 안의 자산 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "자산 목록 조회 성공")
    })
    List<AssetBalance> getAssets();

    @GetMapping("/symbols")
    @Operation(summary = "거래 가능 심볼 목록 조회", description = "거래소에서 현재 거래 가능한 심볼 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "심볼 목록 조회 성공")
    })
    List<ExchangeSymbol> getSymbols();

    @GetMapping("/price")
    @Operation(summary = "현재가 조회", description = "단일 심볼의 현재 가격을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재가 조회 성공")
    })
    SymbolPrice getPrice(@RequestParam @Parameter(description = "Binance Futures 심볼", example = "BTCUSDT") String symbol);

    @GetMapping("/positions")
    @Operation(summary = "현재 포지션 목록 조회", description = "현재 계정이 보유 중인 포지션 목록을 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포지션 목록 조회 성공")
    })
    List<PositionRisk> getPositions();

    @GetMapping("/atr/order/preview")
    @Operation(summary = "ATR 주문 미리보기", description = "ATR 규칙으로 주문 수량과 손절 거리, 필요 증거금을 미리 계산한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ATR 미리보기 성공")
    })
    AtrOrderPreview previewAtrOrder(@ParameterObject AtrOrderRequestDto dto);

    @PostMapping("/atr/order")
    @Operation(summary = "ATR 기준 주문 실행", description = "ATR 기준으로 계산한 수량으로 실제 Binance 주문을 실행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ATR 주문 실행 성공")
    })
    Order orderByAtr(@ParameterObject AtrOrderRequestDto dto, @Parameter(hidden = true) HttpSession session);

    @PostMapping("/positions/liquidate")
    @Operation(summary = "포지션 시장가 청산", description = "현재 보유 중인 특정 심볼 포지션을 시장가 reduceOnly 주문으로 청산한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "포지션 청산 성공")
    })
    Order liquidatePosition(@Parameter(description = "청산할 심볼", example = "BTCUSDT") String symbol);

    @PostMapping("/order")
    @Operation(summary = "일반 주문 실행", description = "사용자가 직접 지정한 수량과 가격으로 일반 주문을 실행한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "일반 주문 실행 성공")
    })
    Order order(@ParameterObject OrderRequestDto dto, @Parameter(hidden = true) HttpSession session);

    @GetMapping("/order")
    @Operation(summary = "서비스 주문 이력 조회", description = "서비스 내부에 저장된 사용자 주문 이력을 심볼 기준으로 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 이력 조회 성공")
    })
    List<UserOrder> getOrders(@RequestParam @Parameter(description = "조회할 심볼", example = "BTCUSDT") String symbol);

    @GetMapping("/order/details")
    @Operation(summary = "주문 상세 조회", description = "거래소에 저장된 단일 주문의 최신 상세 상태를 조회한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 상세 조회 성공")
    })
    Order getOrderDetail(
            @RequestParam @Parameter(description = "조회할 심볼", example = "BTCUSDT") String symbol,
            @RequestParam @Parameter(description = "조회할 주문 ID") String orderId);

    @PostMapping("/order/cancel")
    @Operation(summary = "주문 취소", description = "거래소 미체결 주문을 취소하고 서비스 주문 이력 상태를 갱신한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주문 취소 성공")
    })
    Order cancelOrder(
            @RequestParam @Parameter(description = "취소할 주문 심볼", example = "BTCUSDT") String symbol,
            @RequestParam @Parameter(description = "취소할 주문 ID") String orderId);
}
