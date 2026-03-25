package st.indicator.stindicator.presentation.controller;

import com.java.candle.Candle;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.application.service.ClientService;
import st.indicator.stindicator.presentation.dto.CandleRequestDto;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("client")
public class ClientController {
    private final ClientService clientService;
    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("candles")
    public List<Candle> getCandles(CandleRequestDto dto) {
        return clientService.getCandles(dto.toCommand());
    }

    @GetMapping("balances")
    public BigDecimal getBalance() {
        return clientService.getBalance();
    }
}
