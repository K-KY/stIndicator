package st.indicator.stindicator.domain.utils.candle;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpResponse;
import java.util.List;

public class CandleMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Candle> map(HttpResponse<String> response) throws JsonProcessingException {
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }

    public List<Candle> map(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, new TypeReference<>() {});

    }
}
