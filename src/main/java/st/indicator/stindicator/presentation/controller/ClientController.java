package st.indicator.stindicator.presentation.controller;

import com.java.candle.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.ClientService;
import st.indicator.stindicator.application.service.OrderService;
import st.indicator.stindicator.infra.connector.entity.OrderEntity;
import st.indicator.stindicator.presentation.dto.CandleRequestDto;
import st.indicator.stindicator.presentation.dto.OrderRequestDto;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("client")
public class ClientController {
    private final ClientService clientService;
    private final OrderService orderService;
    public ClientController(ClientService clientService, OrderService orderService) {
        this.clientService = clientService;
        this.orderService = orderService;
    }

    @GetMapping("candles")
    public List<Candle> getCandles(CandleRequestDto dto) {
        return clientService.getCandles(dto.toCommand());
    }

    @GetMapping("balances")
    public BigDecimal getBalance() {
        return clientService.getBalance();
    }

    @GetMapping("atrs")
    public BigDecimal getAtr(CandleRequestDto dto) {
        return clientService.getAtr(dto.toCommand());
    }

    @PostMapping("order")
    public OrderEntity order(OrderRequestDto dto) {
        OrderEntity order = clientService.order(dto.toCommand());
        orderService.save(order.getOrderId(), dto.toCommand());// 사용자 주문 저장
        return order;
    }
}
