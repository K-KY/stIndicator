package st.indicator.stindicator.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import st.indicator.stindicator.domain.utils.client.ExchangeClient;

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
