/**
 * 지도 화면 상태 생성기입니다.
 * 검색·주변·즐겨찾기·경로 마커를 분리해 한 기능의 초기화가 다른 표시를 지우지 않게 합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};

    function createMapState() {
        return {
            map: null,
            places: null,
            geocoder: null,
            currentPosition: null,
            currentAccuracy: null,
            currentMarker: null,
            favoriteMarker: null,
            resultMarkers: [],
            searchResults: [],
            nearbyMarkers: [],
            nearbyResults: [],
            nearbyCategoryCode: "",
            nearbyCategoryLabel: "",
            nearbySearchCenter: null,
            nearbySearchLevel: null,
            nearbyRequestVersion: 0,
            nearbySearching: false,
            favorites: [],
            favoriteByKey: new Map(),
            favoriteMutationVersion: 0,
            routePolylines: [],
            routeBounds: null,
            routeMarkers: {
                start: null,
                end: null
            },
            infoWindow: null,
            selectedPlace: null,
            lastBounds: null,
            activeSidebarTab: "search"
        };
    }

    namespace.state = Object.freeze({ createMapState });
})();
