package com.siyan1234.itproject2nd.map.support;

import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * 카카오 API가 4xx/5xx 응답을 반환했을 때 사용하는 예외입니다.
 */
@Getter
public class KakaoMapApiException extends RuntimeException {

    private final Integer statusCode;
    private final JsonNode responseBody;

    public KakaoMapApiException(String message, Integer statusCode, JsonNode responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
}
