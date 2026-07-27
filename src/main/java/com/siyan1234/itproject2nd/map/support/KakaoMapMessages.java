package com.siyan1234.itproject2nd.map.support;

/**
 * 카카오맵 기능에서 사용하는 사용자 안내 메시지입니다.
 */
public final class KakaoMapMessages {

    public static final String API_KEY_MISSING = "카카오 REST API 키가 설정되어 있지 않습니다.";
    public static final String KAKAO_API_ERROR = "카카오 API 요청 중 오류가 발생했습니다.";

    public static final String ADDRESS_QUERY_REQUIRED = "주소 검색어를 입력해주세요.";
    public static final String COORDINATE_REQUIRED = "x, y 좌표를 모두 입력해주세요.";
    public static final String PLACE_QUERY_REQUIRED = "장소 검색어를 입력해주세요.";
    public static final String STATIC_MAP_COORDINATE_REQUIRED = "정적 지도를 조회할 x, y 좌표를 모두 입력해주세요.";
    public static final String ROUTE_COORDINATE_REQUIRED = "출발지와 도착지 좌표를 모두 입력해주세요.";

    public static final String STATIC_MAP_SUCCESS = "정적 지도 조회가 완료되었습니다.";
    public static final String ROUTE_SUCCESS = "경로 조회가 완료되었습니다.";

    private KakaoMapMessages() {
    }
}
