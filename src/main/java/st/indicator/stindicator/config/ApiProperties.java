package st.indicator.stindicator.config;

import com.java.client.BinanceClient;
import com.java.client.ExchangeClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;

@Component
public class ApiProperties {
    @Value("${BINANCE_API_KEY}")
    private String apiKey;
    @Value("${BINANCE_API_SECRET}")
    private String apiSecret;

    public ExchangeClient getBinanceExchangeClient() {
        return new BinanceClient(apiKey, apiSecret, HttpClient.newHttpClient());
    }
}
