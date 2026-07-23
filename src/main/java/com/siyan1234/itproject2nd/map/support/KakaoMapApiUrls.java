package com.siyan1234.itproject2nd.map.support;

/**
 * 카카오 지도/로컬 REST API URL 상수입니다.
 */
public final class KakaoMapApiUrls {

    /** 주소를 좌표로 변환 */
    public static final String ADDRESS_SEARCH = "https://dapi.kakao.com/v2/local/search/address.json";

    /** 좌표를 주소로 변환 */
    public static final String COORD_TO_ADDRESS = "https://dapi.kakao.com/v2/local/geo/coord2address.json";

    /** 키워드로 장소 검색 */
    public static final String PLACE_KEYWORD_SEARCH = "https://dapi.kakao.com/v2/local/search/keyword.json";

    /** 정적 지도 조회: 2차 패치에서 사용 예정 */
    public static final String STATIC_MAP = "https://dapi.kakao.com/v2/maps/staticmap";

    /** 경로 조회: 3차 패치에서 사용 예정 */
    public static final String PUBLIC_TRAFFIC_ROUTE = "https://dapi.kakao.com/v2/routing/publictraffic";
    public static final String WALK_ROUTE = "https://dapi.kakao.com/v2/routing/walk";

    private KakaoMapApiUrls() {
    }
}
