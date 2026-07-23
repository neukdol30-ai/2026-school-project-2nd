package com.siyan1234.itproject2nd.map.service;

import com.siyan1234.itproject2nd.map.client.KakaoMapClient;
import com.siyan1234.itproject2nd.map.dto.KakaoMapActionResponseDto;
import com.siyan1234.itproject2nd.map.support.KakaoMapApiException;
import com.siyan1234.itproject2nd.map.support.KakaoMapApiProperties;
import com.siyan1234.itproject2nd.map.support.KakaoMapApiUrls;
import com.siyan1234.itproject2nd.map.support.KakaoMapMessages;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.JsonNode;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 카카오맵 1차 기능 서비스입니다.
 *
 * 1차 범위:
 * - 주소 -> 좌표 변환
 * - 좌표 -> 주소 변환
 * - 키워드 장소 검색
 */
@Service
@RequiredArgsConstructor
public class KakaoMapService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_ADDRESS_SIZE = 10;
    private static final int DEFAULT_PLACE_SIZE = 10;
    private static final int MAX_ADDRESS_SIZE = 30;
    private static final int MAX_PLACE_SIZE = 15;
    private static final int MAX_PAGE = 45;

    private final KakaoMapClient kakaoMapClient;

    public KakaoMapActionResponseDto searchAddress(String query, int page, int size) {
        if (!StringUtils.hasText(query)) {
            return KakaoMapActionResponseDto.failure(KakaoMapMessages.ADDRESS_QUERY_REQUIRED);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("query", query.trim());
        parameters.put("page", normalizeNumber(page, DEFAULT_PAGE, 1, MAX_PAGE));
        parameters.put("size", normalizeNumber(size, DEFAULT_ADDRESS_SIZE, 1, MAX_ADDRESS_SIZE));

        return callKakao("주소 검색이 완료되었습니다.", KakaoMapApiUrls.ADDRESS_SEARCH, parameters);
    }

    public KakaoMapActionResponseDto coordToAddress(String x, String y) {
        if (!StringUtils.hasText(x) || !StringUtils.hasText(y)) {
            return KakaoMapActionResponseDto.failure(KakaoMapMessages.COORDINATE_REQUIRED);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("x", x.trim());
        parameters.put("y", y.trim());
        parameters.put("input_coord", "WGS84");

        return callKakao("좌표 주소 변환이 완료되었습니다.", KakaoMapApiUrls.COORD_TO_ADDRESS, parameters);
    }

    public KakaoMapActionResponseDto searchPlaces(String query,
                                                   String x,
                                                   String y,
                                                   Integer radius,
                                                   Integer page,
                                                   Integer size,
                                                   String sort) {
        if (!StringUtils.hasText(query)) {
            return KakaoMapActionResponseDto.failure(KakaoMapMessages.PLACE_QUERY_REQUIRED);
        }

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("query", query.trim());
        parameters.put("x", blankToNull(x));
        parameters.put("y", blankToNull(y));
        parameters.put("radius", radius);
        parameters.put("page", normalizeNumber(page, DEFAULT_PAGE, 1, MAX_PAGE));
        parameters.put("size", normalizeNumber(size, DEFAULT_PLACE_SIZE, 1, MAX_PLACE_SIZE));
        parameters.put("sort", normalizeSort(sort));

        return callKakao("장소 검색이 완료되었습니다.", KakaoMapApiUrls.PLACE_KEYWORD_SEARCH, parameters);
    }

    private KakaoMapActionResponseDto callKakao(String successMessage, String url, Map<String, ?> parameters) {
        try {
            JsonNode data = kakaoMapClient.get(url, parameters);
            return KakaoMapActionResponseDto.success(successMessage, data);
        } catch (KakaoMapApiException e) {
            return KakaoMapActionResponseDto.failure(e.getMessage(), e.getStatusCode(), e.getResponseBody());
        } catch (IllegalStateException e) {
            return KakaoMapActionResponseDto.failure(e.getMessage());
        }
    }

    private int normalizeNumber(Integer value, int defaultValue, int min, int max) {
        int normalized = value == null ? defaultValue : value;
        return Math.max(min, Math.min(max, normalized));
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeSort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return "accuracy";
        }

        if ("distance".equalsIgnoreCase(sort)) {
            return "distance";
        }

        return "accuracy";
    }
}
