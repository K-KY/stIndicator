package st.indicator.stindicator.config;

import com.java.client.ExchangeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class appConfig {
    private final ApiProperties properties;

    public appConfig(ApiProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ExchangeClient getBinanceExchangeClient() {
        return properties.getBinanceExchangeClient();
    }
}
