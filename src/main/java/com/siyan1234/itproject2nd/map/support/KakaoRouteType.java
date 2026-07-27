package com.siyan1234.itproject2nd.map.support;

import lombok.Getter;

/**
 * 카카오맵 경로 조회 종류입니다.
 *
 * publictraffic: 대중교통
 * walk: 도보
 * bicycle: 자전거
 */
@Getter
public enum KakaoRouteType {

    PUBLICTRANSPORT("publictraffic", "대중교통", KakaoMapApiUrls.PUBLIC_TRAFFIC_ROUTE),
    WALK("walk", "도보", KakaoMapApiUrls.WALK_ROUTE),
    BICYCLE("bicycle", "자전거", KakaoMapApiUrls.BICYCLE_ROUTE);

    private final String code;
    private final String label;
    private final String url;

    KakaoRouteType(String code, String label, String url) {
        this.code = code;
        this.label = label;
        this.url = url;
    }

    public static KakaoRouteType from(String value) {
        if (value == null) {
            return PUBLICTRANSPORT;
        }

        for (KakaoRouteType type : values()) {
            if (type.code.equalsIgnoreCase(value)) {
                return type;
            }
        }

        return PUBLICTRANSPORT;
    }

    public boolean isPublicTraffic() {
        return this == PUBLICTRANSPORT;
    }

    public boolean isWalk() {
        return this == WALK;
    }

    public boolean isBicycle() {
        return this == BICYCLE;
    }
}
