package com.siyan1234.itproject2nd.map.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

/**
 * 프론트엔드에 내려주는 카카오맵 공통 응답 DTO입니다.
 *
 * data에는 카카오 API 원본 JSON을 그대로 담습니다.
 * 화면에서는 documents, meta 등을 그대로 확인할 수 있습니다.
 */
@Getter
@AllArgsConstructor
public class KakaoMapActionResponseDto {

    private boolean success;
    private String message;
    private Integer kakaoStatus;
    private JsonNode data;

    public static KakaoMapActionResponseDto success(String message, JsonNode data) {
        return new KakaoMapActionResponseDto(true, message, 200, data);
    }

    public static KakaoMapActionResponseDto failure(String message) {
        return new KakaoMapActionResponseDto(false, message, null, null);
    }

    public static KakaoMapActionResponseDto failure(String message, Integer kakaoStatus, JsonNode data) {
        return new KakaoMapActionResponseDto(false, message, kakaoStatus, data);
    }
}
