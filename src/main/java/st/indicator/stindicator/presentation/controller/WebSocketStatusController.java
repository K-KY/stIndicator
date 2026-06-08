package st.indicator.stindicator.presentation.controller;

import org.springframework.web.bind.annotation.RestController;
import st.indicator.stindicator.infra.ws.MultiPlexManager;
import st.indicator.stindicator.presentation.dto.WebSocketStatusResponseDto;
import st.indicator.stindicator.presentation.ws.handler.MultiPlexHandler;

@RestController
public class WebSocketStatusController implements WebSocketStatusApi {
    private final MultiPlexHandler multiPlexHandler;
    private final MultiPlexManager multiPlexManager;

    public WebSocketStatusController(MultiPlexHandler multiPlexHandler, MultiPlexManager multiPlexManager) {
        this.multiPlexHandler = multiPlexHandler;
        this.multiPlexManager = multiPlexManager;
    }

    @Override
    public WebSocketStatusResponseDto status() {
        return new WebSocketStatusResponseDto(
                multiPlexHandler.activeSessionCount(),
                multiPlexManager.activeConnectionCount(),
                multiPlexManager.subscriptionCount()
        );
    }
}
