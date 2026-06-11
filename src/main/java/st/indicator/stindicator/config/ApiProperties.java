package st.indicator.stindicator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import st.indicator.stindicator.domain.utils.client.BinanceClient;
import st.indicator.stindicator.domain.utils.client.ExchangeClient;

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

    public String getApiKey() {
        return apiKey;
    }
}
