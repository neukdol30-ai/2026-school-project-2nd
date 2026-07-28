/**
 * 지도 화면에서 공통으로 사용하는 API 주소와 표시 제한값입니다.
 * 기능 파일마다 같은 문자열을 중복 선언하지 않도록 한 곳에서 관리합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};

    namespace.config = Object.freeze({
        ROUTE_API: "/api/kakao-map/route",
        FAVORITE_API: "/api/kakao-map/favorites",
        LOCATION_OPTIONS: Object.freeze({
            enableHighAccuracy: true,
            timeout: 15000,
            maximumAge: 0
        }),
        DEFAULT_MAP_LEVEL: 4,
        MAX_MAP_LEVEL: 10,
        MAX_PUBLIC_ROUTE_CANDIDATES: 5,
        NEARBY_SEARCH_RADIUS: 3000,
        NEARBY_CATEGORY_LABELS: Object.freeze({
            FD6: "음식점",
            CE7: "카페",
            CS2: "편의점",
            PK6: "주차장",
            OL7: "주유소",
            SW8: "지하철역",
            HP8: "병원",
            PM9: "약국"
        }),
        ROUTE_LINE_OPTIONS: Object.freeze({
            strokeWeight: 6,
            strokeColor: "#2563eb",
            strokeOpacity: 0.86,
            strokeStyle: "solid"
        })
    });
})();
