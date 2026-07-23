package com.siyan1234.itproject2nd.map.client;

import com.siyan1234.itproject2nd.map.support.KakaoMapApiException;
import com.siyan1234.itproject2nd.map.support.KakaoMapApiProperties;
import com.siyan1234.itproject2nd.map.support.KakaoMapMessages;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import tools.jackson.core.JacksonException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 카카오 REST API를 실제로 호출하는 클라이언트입니다.
 *
 * REST API 키가 브라우저에 노출되지 않도록 반드시 서버에서만 호출합니다.
 */
@Component
public class KakaoMapClient {

    private final KakaoMapApiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public KakaoMapClient(KakaoMapApiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .build();
    }

    public JsonNode get(String url, Map<String, ?> parameters) {
        if (!properties.hasRestApiKey()) {
            throw new IllegalStateException(KakaoMapMessages.API_KEY_MISSING);
        }

        URI uri = buildUri(url, parameters);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()))
                .header("Authorization", properties.authorizationHeaderValue())
                .header("Accept", "application/json")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode body = parseBody(response.body());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return body;
            }

            throw new KakaoMapApiException("카카오 API 요청 실패: HTTP " + response.statusCode(), response.statusCode(), body);
        } catch (IOException e) {
            throw new IllegalStateException(KakaoMapMessages.KAKAO_API_ERROR + " " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(KakaoMapMessages.KAKAO_API_ERROR + " 요청이 중단되었습니다.", e);
        }
    }

    private URI buildUri(String url, Map<String, ?> parameters) {
        String queryString = parameters.entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> StringUtils.hasText(String.valueOf(entry.getValue())))
                .map(entry -> encode(entry.getKey()) + "=" + encode(String.valueOf(entry.getValue())))
                .collect(Collectors.joining("&"));

        if (!StringUtils.hasText(queryString)) {
            return URI.create(url);
        }

        return URI.create(url + "?" + queryString);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private JsonNode parseBody(String body) {
        if (!StringUtils.hasText(body)) {
            return JsonNodeFactory.instance.objectNode();
        }

        try {
            return objectMapper.readTree(body);
        } catch (JacksonException e) {
            ObjectNode node = JsonNodeFactory.instance.objectNode();
            node.put("raw", body);
            return node;
        }
    }
}
