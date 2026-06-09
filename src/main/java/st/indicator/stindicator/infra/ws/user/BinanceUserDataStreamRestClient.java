package st.indicator.stindicator.infra.ws.user;

import org.springframework.stereotype.Component;
import st.indicator.stindicator.config.ApiProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class BinanceUserDataStreamRestClient {
    private static final String LISTEN_KEY_URL = "https://fapi.binance.com/fapi/v1/listenKey";
    private static final String API_KEY_HEADER = "X-MBX-APIKEY";
    private final ApiProperties apiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public BinanceUserDataStreamRestClient(ApiProperties apiProperties, ObjectMapper objectMapper) {
        this.apiProperties = apiProperties;
        this.objectMapper = objectMapper;
    }

    public String start() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LISTEN_KEY_URL))
                .header(API_KEY_HEADER, apiProperties.getApiKey())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        JsonNode root = send(request, "start user data stream");
        JsonNode listenKey = root.get("listenKey");
        if (listenKey == null || listenKey.isNull() || listenKey.asText().isBlank()) {
            throw new IllegalStateException("Binance listenKey response missing listenKey");
        }
        return listenKey.asText();
    }

    public void keepAlive() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LISTEN_KEY_URL))
                .header(API_KEY_HEADER, apiProperties.getApiKey())
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();
        send(request, "keepalive user data stream");
    }

    public boolean isConfigured() {
        String apiKey = apiProperties.getApiKey();
        return apiKey != null && !apiKey.isBlank();
    }

    private JsonNode send(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("code") && root.has("msg")) {
                throw new IllegalStateException("Binance " + operation + " failed code="
                        + root.get("code").asText() + ", msg=" + root.get("msg").asText());
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Binance " + operation + " failed status="
                        + response.statusCode() + ", body=" + response.body());
            }
            return root;
        } catch (IOException e) {
            throw new IllegalStateException("Binance " + operation + " I/O failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Binance " + operation + " interrupted", e);
        }
    }
}
