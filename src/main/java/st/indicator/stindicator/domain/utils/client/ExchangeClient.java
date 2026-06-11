package st.indicator.stindicator.domain.utils.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public interface ExchangeClient {
    String get(String path, Map<String, String> params)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException;
    JsonNode get(String path, Map<String, String> params, String property)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException;

    String post(String path, Map<String, String> params)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, InterruptedException;
    String delete(String path, Map<String, String> params)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, InterruptedException;
}
