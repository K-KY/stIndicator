package st.indicator.stindicator.presentation.ws.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import st.indicator.stindicator.presentation.ws.handler.MultiPlexHandler;

@Configuration
@EnableWebSocket
public class MultiPlexWebsocketConfig implements WebSocketConfigurer {
    private final MultiPlexHandler handler;

    public MultiPlexWebsocketConfig(MultiPlexHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/mp")
                .setAllowedOrigins("*");
    }
}
