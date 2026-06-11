package st.indicator.stindicator.domain.utils.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import st.indicator.stindicator.domain.utils.uri.UriBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class BinanceClient implements ExchangeClient {

    private static final String SIGNATURE = "signature";
    private static final String X_MBX_APIKEY = "X-MBX-APIKEY";
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private final String apiKey;
    private final String secret;
    private final HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public BinanceClient(String apiKey, String secret, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.secret = secret;
        this.client = httpClient;
    }

    /**
     *
     * @param path - 호출할 api
     * @param params - 호출 시 사용될 쿼리 파라미터
     * @return - api 호출 후 반환된 전체 결과 반환
     * @throws IOException
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Override
    public String get(String path, Map<String, String> params) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        // signature 생성
        String signature = getSignature(urIBuilder(params));

        // query build
        String url = UriBuilder.builder()
                .path(urLBuilder(path, params))
                .query(SIGNATURE, signature)
                .build();

        // http 요청
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(X_MBX_APIKEY, apiKey)
                .GET()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

        return send.body();
    }

    @Override
    public JsonNode get(String path, Map<String, String> params, String property)
            throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        String s = get(path, params);
        return mapper.readTree(s).get(property);
    }

    @Override
    public String post(String path, Map<String, String> params)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, InterruptedException {

        String bodyString = urIBuilder(params);

        String signature = getSignature(bodyString);

        String fullBody = bodyString + "&" + SIGNATURE + "=" + signature;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .header(X_MBX_APIKEY, apiKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(fullBody))
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

        return send.body();
    }

    public String delete(String path, Map<String, String> params) throws IOException, InterruptedException, NoSuchAlgorithmException, InvalidKeyException {
        // signature 생성
        String signature = getSignature(urIBuilder(params));

        // query build
        String url = UriBuilder.builder()
                .path(urLBuilder(path, params))
                .query(SIGNATURE, signature)
                .build();

        // http 요청
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header(X_MBX_APIKEY, apiKey)
                .DELETE()
                .build();

        HttpResponse<String> send = client.send(request, HttpResponse.BodyHandlers.ofString());

        return send.body();
    }

    //uri를 HmacSHA256로 암호화
    private String getSignature(String uri) throws NoSuchAlgorithmException, InvalidKeyException {
        SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256);
        return hmacSha256(uri, keySpec);
    }

    //암호화
    public String hmacSha256(String uri, SecretKeySpec keySpec) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA_256);

        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(uri.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : rawHmac) {
            String s= Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                hex.append('0');
            }
            hex.append(s);
        }
        return hex.toString();
    }

    /**
     *
     * @param path - 호출할 api
     * @param params - 호출 시 사용될 쿼리 파라미터
     * @return path?key=value&key=value...
     */
    private String urLBuilder(String path, Map<String ,String> params) {
        UriBuilder.Builder url = UriBuilder.builder().path(path);

        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.query(entry.getKey(), entry.getValue());
        }
        return url.build();
    }

    /**
     *
     * @param params uri로 빌드할 쿼리 파라미터
     * @return uri를 빌드해서 반환한다.<br>
     * 반환 형식은 key=value&key=value...
     */
    private String urIBuilder(Map<String ,String> params) {
        UriBuilder.Builder url = UriBuilder.builder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            url.query(entry.getKey(), entry.getValue());
        }
        return url.build();
    }
}