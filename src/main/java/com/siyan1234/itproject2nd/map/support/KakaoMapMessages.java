package com.siyan1234.itproject2nd.map.support;

/**
 * 카카오맵 기능에서 사용하는 사용자 안내 메시지입니다.
 */
public final class KakaoMapMessages {

    public static final String API_KEY_MISSING = "카카오 REST API 키가 설정되지 않았습니다. KAKAO_REST_API_KEY 환경변수를 확인해 주세요.";
    public static final String ADDRESS_QUERY_REQUIRED = "주소 검색어를 입력해 주세요.";
    public static final String PLACE_QUERY_REQUIRED = "장소 검색어를 입력해 주세요.";
    public static final String COORDINATE_REQUIRED = "x, y 좌표를 모두 입력해 주세요.";
    public static final String KAKAO_API_ERROR = "카카오 API 요청 중 오류가 발생했습니다.";

    private KakaoMapMessages() {
    }
}
