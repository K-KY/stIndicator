package st.indicator.stindicator.presentation.ws.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.MonitorService;
import st.indicator.stindicator.application.service.OrderService;
import st.indicator.stindicator.domain.entity.Order;
import st.indicator.stindicator.domain.entity.PositionMonitor;
import st.indicator.stindicator.presentation.ws.dto.MonitorEventType;
import st.indicator.stindicator.presentation.ws.dto.MonitorSocketEventDto;
import st.indicator.stindicator.presentation.ws.dto.LimitOrderRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MarketOrderRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MonitorStartRequestDto;
import st.indicator.stindicator.presentation.ws.dto.MonitorStopRequestDto;
import st.indicator.stindicator.presentation.ws.dto.OrderExecutionResponseDto;
import st.indicator.stindicator.presentation.ws.dto.PositionMonitorResponseDto;
import st.indicator.stindicator.presentation.ws.service.PositionMonitorService;

import java.util.List;

@RestController
public class MonitorController implements MonitorApi {
    private static final Logger log = LoggerFactory.getLogger(MonitorController.class);
    private final PositionMonitorService positionMonitorService;
    private final OrderService orderService;
    private final MonitorService monitorService;

    public MonitorController(PositionMonitorService positionMonitorService, OrderService orderService,
                             MonitorService monitorService) {
        this.positionMonitorService = positionMonitorService;
        this.orderService = orderService;
        this.monitorService = monitorService;
    }

    @Override
    public PositionMonitorResponseDto start(MonitorStartRequestDto request) {
        log.info("monitor start userId={}, symbol={}", request.getUserId(), request.getSymbol());
        PositionMonitor positionMonitor = positionMonitorService.start(request);
        return PositionMonitorResponseDto.from(positionMonitor);
    }

    @Override
    public PositionMonitorResponseDto stop(MonitorStopRequestDto request) {
        log.info("monitor stop userId={}, monitorId={}", request.getUserId(), request.getMonitorId());
        return positionMonitorService.stop(request.getUserId(), request.getMonitorId())
                .map(PositionMonitorResponseDto::from)
                .orElse(null);
    }

    @Override
    public List<PositionMonitorResponseDto> list(Long userId) {
        log.info("monitor list userId={}", userId);
        return positionMonitorService.list(userId).stream()
                .map(PositionMonitorResponseDto::from)
                .toList();
    }

    @Override
    public OrderExecutionResponseDto marketOrder(MarketOrderRequestDto request) {
        log.info("market order userId={}, symbol={}, side={}", request.getUserId(), request.getSymbol(), request.getSide());
        Order order = orderService.placeMarketOrder(request.getUserId(), request.getSymbol(), request.getSide(), request.getQuantity());
        OrderExecutionResponseDto response = OrderExecutionResponseDto.from(order);
        monitorService.publishMonitorEvent(order.getSymbol(), new MonitorSocketEventDto(
                MonitorEventType.ORDER_EXECUTED,
                request.getUserId(),
                order.getSymbol(),
                null,
                response
        ));
        return response;
    }

    @Override
    public OrderExecutionResponseDto limitOrder(LimitOrderRequestDto request) {
        log.info("limit order userId={}, symbol={}, side={}", request.getUserId(), request.getSymbol(), request.getSide());
        Order order = orderService.placeLimitOrder(
                request.getUserId(),
                request.getSymbol(),
                request.getSide(),
                request.getQuantity(),
                request.getPrice(),
                request.getTimeInForce()
        );
        OrderExecutionResponseDto response = OrderExecutionResponseDto.from(order);
        monitorService.publishMonitorEvent(order.getSymbol(), new MonitorSocketEventDto(
                MonitorEventType.ORDER_EXECUTED,
                request.getUserId(),
                order.getSymbol(),
                null,
                response
        ));
        return response;
    }
}
