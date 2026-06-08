package st.indicator.stindicator.presentation.controller;

import com.java.candle.Candle;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.dto.AtrOrderPreview;
import st.indicator.stindicator.application.service.ClientService;
import st.indicator.stindicator.application.service.OrderService;
import st.indicator.stindicator.application.service.SessionUser;
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

@RestController
public class ClientController implements ClientApi {
    private static final Logger log = LoggerFactory.getLogger(ClientController.class);
    private final ClientService clientService;
    private final OrderService orderService;
    public ClientController(ClientService clientService, OrderService orderService) {
        this.clientService = clientService;
        this.orderService = orderService;
    }

    /**
     * 지정한 심볼과 주기로 과거 캔들 목록을 조회한다.
     * ATR 계산, 화면 차트 표시, 시세 분석의 기초 데이터가 되는 raw candle 조회 엔드포인트다.
     */
    @Override
    public List<Candle> getCandles(CandleRequestDto dto) {
        log.info("request getCandles symbol={}, interval={}, limit={}",
                dto.getSymbol(), dto.getInterval(), dto.getLimit());
        return clientService.getCandles(dto.toCommand());
    }

    /**
     * 선물 계정의 총 지갑 잔고를 조회한다.
     * 개별 자산 목록이 아니라 전체 계정 잔고를 단일 값으로 확인할 때 사용한다.
     */
    @Override
    public BigDecimal getBalance() {
        log.info("request getBalance");
        return clientService.getBalance();
    }

    /**
     * 지정한 심볼/주기의 ATR 값을 계산해 반환한다.
     * 내부적으로 캔들을 먼저 조회한 뒤 서비스 계층에서 ATR을 계산한다.
     */
    @Override
    public BigDecimal getAtr(CandleRequestDto dto) {
        log.info("request getAtr symbol={}, interval={}, limit={}",
                dto.getSymbol(), dto.getInterval(), dto.getLimit());
        return clientService.getAtr(dto.toCommand());
    }

    /**
     * 선물 계정 안의 자산 목록을 조회한다.
     * 지갑 잔고, 가용 잔고, 미실현 손익이 함께 반환되어 리스크 계산의 기준값으로 사용된다.
     */
    @Override
    public List<AssetBalance> getAssets() {
        log.info("request getAssets");
        return clientService.getAssets();
    }

    /**
     * 거래소에서 현재 거래 가능한 심볼 목록을 조회한다.
     * 화면 종목 선택 목록이나 모니터링 대상 선택 UI를 구성할 때 사용한다.
     */
    @Override
    public List<ExchangeSymbol> getSymbols() {
        log.info("request getSymbols");
        return clientService.getExchangeSymbols();
    }

    /**
     * 단일 심볼의 현재 가격을 조회한다.
     * ATR 주문 미리보기에서 entryPrice를 생략했을 때 기본 기준값으로도 사용된다.
     */
    @Override
    public SymbolPrice getPrice(String symbol) {
        log.info("request getPrice symbol={}", symbol);
        return clientService.getPrice(symbol);
    }

    /**
     * 현재 계정이 보유 중인 포지션 목록을 조회한다.
     * 수량, 진입가, 현재가, 손익, 배율 등 포지션 관리에 필요한 핵심 정보를 반환한다.
     */
    @Override
    public List<PositionRisk> getPositions() {
        log.info("request getPositions");
        return clientService.getPositions();
    }

    /**
     * ATR 규칙으로 주문 수량과 손절 거리, 필요 증거금을 미리 계산한다.
     * 실제 주문은 발생시키지 않고, 주문 전에 리스크와 수량을 검토하기 위한 미리보기 엔드포인트다.
     */
    @Override
    public AtrOrderPreview previewAtrOrder(AtrOrderRequestDto dto) {
        log.info("request previewAtrOrder symbol={}, side={}, interval={}, limit={}, atrPeriod={}, riskPercent={}, atrMultiplier={}, leverage={}, type={}, timeInForce={}, entryPrice={}",
                dto.getSymbol(), dto.getSide(), dto.getInterval(), dto.getLimit(), dto.getAtrPeriod(),
                dto.getRiskPercent(), dto.getAtrMultiplier(), dto.getLeverage(), dto.getType(),
                dto.getTimeInForce(), dto.getEntryPrice());
        return clientService.previewAtrOrder(dto.toCommand());
    }

    //테스트용 메서드
    public Order orderByAtr(AtrOrderRequestDto dto) {
        return orderByAtr(dto, null);
    }

    /**
     * ATR 기준으로 계산한 수량으로 실제 Binance 주문을 실행한다.
     * 미리보기 로직을 먼저 수행한 뒤 계산된 quantity를 사용해 주문하고, 결과는 사용자 주문 이력에도 저장한다.
     */
    @Override
    public Order orderByAtr(AtrOrderRequestDto dto, HttpSession session) {
        log.info("request orderByAtr symbol={}, side={}, interval={}, limit={}, atrPeriod={}, riskPercent={}, atrMultiplier={}, leverage={}, type={}, timeInForce={}, entryPrice={}",
                dto.getSymbol(), dto.getSide(), dto.getInterval(), dto.getLimit(), dto.getAtrPeriod(),
                dto.getRiskPercent(), dto.getAtrMultiplier(), dto.getLeverage(), dto.getType(),
                dto.getTimeInForce(), dto.getEntryPrice());
        Order order = clientService.orderByAtr(dto.toCommand());
        Long userId = sessionUserId(session);
        orderService.save(userId, order.getOrderId(), order.getSymbol(), order.getSide(), order.getType(),
                order.getTimeInForce(), toPlainString(order.getOrigQty()), toPlainString(order.getPrice()), order.getStatus());
        return order;
    }

    /**
     * 현재 보유 중인 특정 심볼 포지션을 시장가 reduceOnly 주문으로 청산한다.
     * 롱 포지션이면 SELL, 숏 포지션이면 BUY 방향으로 반대 주문을 만들어 종료한다.
     */
    @Override
    public Order liquidatePosition(String symbol) {
        log.info("request liquidatePosition symbol={}", symbol);
        return clientService.liquidatePosition(symbol);
    }

    /**
     * 사용자가 직접 지정한 수량과 가격으로 일반 주문을 실행한다.
     * ATR 계산 없이 바로 주문하고, 결과는 서비스 내부 주문 이력에도 함께 저장한다.
     */
    public Order order(OrderRequestDto dto) {
        return order(dto, null);
    }

    @Override
    public Order order(OrderRequestDto dto, HttpSession session) {
        log.info("request order symbol={}, side={}, type={}, timeInForce={}, quantity={}, price={}",
                dto.getSymbol(), dto.getSide(), dto.getType(), dto.getTimeInForce(), dto.getQuantity(), dto.getPrice());
        Order order = clientService.order(dto.toCommand());
        orderService.save(sessionUserId(session), order.getOrderId(), order.getSymbol(), order.getSide(), order.getType(),
                order.getTimeInForce(), toPlainString(order.getOrigQty()), toPlainString(order.getPrice()), order.getStatus());// 사용자 주문 저장
        return order;
    }

    /**
     * 서비스 내부에 저장된 사용자 주문 이력을 심볼 기준으로 조회한다.
     * 거래소 원본 주문 조회와 달리, 우리 서비스가 저장한 주문 목록을 확인하는 용도다.
     */
    @Override
    public List<UserOrder> getOrders(String symbol /*특정 사용자,  미체결, 체결 필터 추가 되어야함,*/) {
        log.info("request getOrders symbol={}", symbol);
        return orderService.getOrders(symbol);
    }

    /**
     * 거래소에 저장된 단일 주문의 최신 상세 상태를 조회한다.
     * 주문 ID와 심볼을 사용해 체결 상태, 체결 수량 등 주문 응답 상세를 다시 확인할 때 사용한다.
     */
    @Override
    public Order getOrderDetail(
            String symbol,
            String orderId) {
        log.info("request getOrderDetail symbol={}, orderId={}", symbol, orderId);
        return clientService.getOrderDetail(symbol, orderId);
    }

    @Override
    public Order cancelOrder(String symbol, String orderId) {
        log.info("request cancelOrder symbol={}, orderId={}", symbol, orderId);
        return orderService.cancelOrder(symbol, orderId);
    }

    private String toPlainString(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private Long sessionUserId(HttpSession session) {
        Object userId = session == null ? null : session.getAttribute(SessionUser.USER_ID);
        return userId instanceof Long id ? id : null;
    }
}
