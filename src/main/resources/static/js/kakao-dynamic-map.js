/*
 * kakao-dynamic-map.js
 * 역할: Kakao Map JavaScript SDK 기반 동적 지도 화면을 담당합니다.
 *
 * 화면에는 위도/경도 숫자를 직접 노출하지 않습니다.
 * 좌표는 마커 이동, 경로 조회, 즐겨찾기 저장을 위한 내부 데이터로만 사용합니다.
 */
(() => {
    const ROUTE_API = "/api/kakao-map/route";
    const DEFAULT_CENTER = { y: 37.566826, x: 126.978652 };
    const DEFAULT_LEVEL = 5;

    let map = null;
    let places = null;
    let geocoder = null;
    let infoWindow = null;
    let selectedPlace = null;
    let currentLocationMarker = null;
    let startMarker = null;
    let endMarker = null;
    let searchMarkers = [];

    document.addEventListener("DOMContentLoaded", () => {
        if (!isKakaoReady()) {
            showMessage("카카오맵 SDK를 불러오지 못했습니다. JavaScript 키와 도메인 설정을 확인해 주세요.", "error");
            return;
        }

        initMap();
        bindEvents();
    });

    function isKakaoReady() {
        return window.kakao && kakao.maps && kakao.maps.services;
    }

    function initMap() {
        const container = document.querySelector("[data-kakao-map]");
        if (!container) {
            return;
        }

        const center = new kakao.maps.LatLng(DEFAULT_CENTER.y, DEFAULT_CENTER.x);
        map = new kakao.maps.Map(container, {
            center,
            level: DEFAULT_LEVEL
        });

        map.addControl(new kakao.maps.ZoomControl(), kakao.maps.ControlPosition.RIGHT);
        places = new kakao.maps.services.Places();
        geocoder = new kakao.maps.services.Geocoder();
        infoWindow = new kakao.maps.InfoWindow({ zIndex: 10 });
    }

    function bindEvents() {
        document.querySelector("[data-map-search-form]")?.addEventListener("submit", handleSearchSubmit);
        document.querySelector("[data-route-form]")?.addEventListener("submit", handleRouteSubmit);
        document.querySelector("[data-current-location-button]")?.addEventListener("click", moveToCurrentLocation);
        document.querySelector("[data-zoom-in-button]")?.addEventListener("click", () => changeZoom(-1));
        document.querySelector("[data-zoom-out-button]")?.addEventListener("click", () => changeZoom(1));
        document.querySelector("[data-selected-start]")?.addEventListener("click", () => setRoutePoint("start", selectedPlace));
        document.querySelector("[data-selected-end]")?.addEventListener("click", () => setRoutePoint("end", selectedPlace));

        document.querySelector('[data-route-form] [name="type"]')?.addEventListener("change", toggleWalkOption);
        toggleWalkOption();
    }

    function handleSearchSubmit(event) {
        event.preventDefault();

        const form = event.currentTarget;
        const query = form.querySelector('[name="query"]')?.value?.trim();
        const searchType = form.querySelector('[name="searchType"]:checked')?.value || "keyword";

        if (!query) {
            showMessage("검색어를 입력해 주세요.", "error");
            return;
        }

        showMessage("검색 중입니다.", "");

        if (searchType === "address") {
            searchAddress(query);
            return;
        }

        searchKeyword(query);
    }

    function searchKeyword(query) {
        places.keywordSearch(query, (data, status) => {
            if (status !== kakao.maps.services.Status.OK) {
                clearSearchResults("검색 결과가 없습니다.");
                showMessage("검색 결과가 없습니다.", "");
                return;
            }

            const results = data.map((item) => normalizePlaceFromKeyword(item));
            renderSearchResults(results);
            renderSearchMarkers(results);
            fitMapToPlaces(results);
            showMessage("장소 검색이 완료되었습니다.", "success");
        });
    }

    function searchAddress(query) {
        geocoder.addressSearch(query, (data, status) => {
            if (status !== kakao.maps.services.Status.OK) {
                clearSearchResults("주소 검색 결과가 없습니다.");
                showMessage("주소 검색 결과가 없습니다.", "");
                return;
            }

            const results = data.map((item) => normalizePlaceFromAddress(item));
            renderSearchResults(results);
            renderSearchMarkers(results);
            fitMapToPlaces(results);
            showMessage("주소 검색이 완료되었습니다.", "success");
        });
    }

    function normalizePlaceFromKeyword(item) {
        return {
            id: item.id || `${item.x}-${item.y}`,
            name: item.place_name || "장소명 없음",
            address: item.road_address_name || item.address_name || "주소 정보 없음",
            category: item.category_name || "",
            phone: item.phone || "",
            placeUrl: item.place_url || "",
            x: Number(item.x),
            y: Number(item.y)
        };
    }

    function normalizePlaceFromAddress(item) {
        const roadAddress = item.road_address?.address_name;
        const address = item.address?.address_name || item.address_name || "주소 정보 없음";

        return {
            id: `${item.x}-${item.y}`,
            name: roadAddress || address,
            address,
            category: "주소",
            phone: "",
            placeUrl: "",
            x: Number(item.x),
            y: Number(item.y)
        };
    }

    function renderSearchResults(results) {
        const list = document.querySelector("[data-place-result-list]");
        const count = document.querySelector("[data-result-count]");

        if (count) {
            count.textContent = `${results.length}개`;
        }

        if (!list) {
            return;
        }

        if (results.length === 0) {
            list.innerHTML = '<div class="empty-result">검색 결과가 없습니다.</div>';
            return;
        }

        list.innerHTML = results.map((place, index) => `
            <article class="place-result-card" data-place-index="${index}">
                <button type="button" class="place-main-button" data-select-place="${index}">
                    <strong>${escapeHtml(place.name)}</strong>
                    <span>${escapeHtml(place.address)}</span>
                    ${place.category ? `<small>${escapeHtml(place.category)}</small>` : ""}
                </button>
                <div class="place-card-actions">
                    <button type="button" data-route-point="start" data-place-index="${index}">출발지</button>
                    <button type="button" data-route-point="end" data-place-index="${index}">도착지</button>
                    <button type="button" data-favorite-placeholder disabled>즐겨찾기</button>
                    ${place.placeUrl ? `<a href="${escapeAttribute(place.placeUrl)}" target="_blank" rel="noopener noreferrer">카카오맵</a>` : ""}
                </div>
            </article>
        `).join("");

        list.querySelectorAll("[data-select-place]").forEach((button) => {
            button.addEventListener("click", () => {
                selectPlace(results[Number(button.dataset.selectPlace)]);
            });
        });

        list.querySelectorAll("[data-route-point]").forEach((button) => {
            button.addEventListener("click", () => {
                const place = results[Number(button.dataset.placeIndex)];
                setRoutePoint(button.dataset.routePoint, place);
            });
        });
    }

    function renderSearchMarkers(results) {
        clearSearchMarkers();

        results.forEach((place) => {
            const marker = new kakao.maps.Marker({
                map,
                position: new kakao.maps.LatLng(place.y, place.x)
            });

            kakao.maps.event.addListener(marker, "click", () => {
                selectPlace(place);
            });

            searchMarkers.push(marker);
        });
    }

    function fitMapToPlaces(results) {
        if (!results.length) {
            return;
        }

        const bounds = new kakao.maps.LatLngBounds();
        results.forEach((place) => bounds.extend(new kakao.maps.LatLng(place.y, place.x)));
        map.setBounds(bounds);
    }

    function selectPlace(place) {
        if (!place) {
            return;
        }

        selectedPlace = place;
        const position = new kakao.maps.LatLng(place.y, place.x);
        map.panTo(position);

        infoWindow.setContent(`<div class="map-info-window">${escapeHtml(place.name)}</div>`);
        infoWindow.setPosition(position);
        infoWindow.open(map);

        updateSelectedPlaceUi(place);
    }

    function updateSelectedPlaceUi(place) {
        setText("[data-selected-place-name]", place.name);
        setText("[data-selected-place-address]", place.address);
        setText("[data-selected-card-name]", place.name);
        setText("[data-selected-card-address]", place.address);

        const card = document.querySelector("[data-selected-place-card]");
        if (card) {
            card.hidden = false;
        }
    }

    function setRoutePoint(type, place) {
        if (!place) {
            showMessage("먼저 장소를 선택해 주세요.", "error");
            return;
        }

        const form = document.querySelector("[data-route-form]");
        if (!form) {
            return;
        }

        const markerPosition = new kakao.maps.LatLng(place.y, place.x);

        if (type === "start") {
            form.querySelector('[name="startX"]').value = place.x;
            form.querySelector('[name="startY"]').value = place.y;
            form.querySelector('[name="startName"]').value = place.name;
            setText("[data-route-start-name]", place.name);
            startMarker = refreshMarker(startMarker, markerPosition, "출발");
            showMessage("출발지가 설정되었습니다.", "success");
            return;
        }

        form.querySelector('[name="endX"]').value = place.x;
        form.querySelector('[name="endY"]').value = place.y;
        form.querySelector('[name="endName"]').value = place.name;
        setText("[data-route-end-name]", place.name);
        endMarker = refreshMarker(endMarker, markerPosition, "도착");
        showMessage("도착지가 설정되었습니다.", "success");
    }

    function refreshMarker(marker, position, title) {
        if (marker) {
            marker.setMap(null);
        }

        return new kakao.maps.Marker({
            map,
            position,
            title
        });
    }

    async function handleRouteSubmit(event) {
        event.preventDefault();

        const form = event.currentTarget;
        const result = document.querySelector("[data-route-result]");
        const payload = {
            type: form.querySelector('[name="type"]')?.value || "publictraffic",
            routeMode: form.querySelector('[name="routeMode"]')?.value || "BROAD_FIRST",
            startX: form.querySelector('[name="startX"]')?.value,
            startY: form.querySelector('[name="startY"]')?.value,
            startName: form.querySelector('[name="startName"]')?.value,
            endX: form.querySelector('[name="endX"]')?.value,
            endY: form.querySelector('[name="endY"]')?.value,
            endName: form.querySelector('[name="endName"]')?.value
        };

        if (!payload.startX || !payload.startY || !payload.endX || !payload.endY) {
            renderRouteMessage("출발지와 도착지를 모두 설정해 주세요.", "error");
            return;
        }

        renderRouteMessage("경로를 조회하는 중입니다.", "");

        try {
            const response = await requestJson(ROUTE_API, payload);
            renderRouteResult(response, payload.type);
            fitRouteBounds(payload);
        } catch (error) {
            if (result) {
                result.innerHTML = routeMessageTemplate(error.message || "경로 조회 중 오류가 발생했습니다.", "error");
            }
        }
    }

    async function requestJson(url, params) {
        const queryString = new URLSearchParams();
        Object.entries(params)
            .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
            .forEach(([key, value]) => queryString.append(key, value));

        const response = await fetch(`${url}?${queryString}`, {
            method: "GET",
            headers: { "Accept": "application/json" }
        });

        if (!response.ok) {
            throw new Error(`서버 요청 실패: HTTP ${response.status}`);
        }

        return response.json();
    }

    function renderRouteResult(response, type) {
        const result = document.querySelector("[data-route-result]");
        if (!result) {
            return;
        }

        if (!response.success) {
            result.innerHTML = routeMessageTemplate(response.message || "경로 조회에 실패했습니다.", "error");
            return;
        }

        const data = response.data || {};
        const status = data.status || "UNKNOWN";

        if (status !== "OK") {
            result.innerHTML = routeMessageTemplate(`경로 조회 결과가 없습니다. 상태: ${status}`, "error");
            return;
        }

        if (type === "publictraffic") {
            result.innerHTML = renderPublicTrafficSummary(response.message, data);
            return;
        }

        result.innerHTML = renderSimpleRouteSummary(response.message, data, type);
    }

    function renderPublicTrafficSummary(message, data) {
        const properties = data.properties || {};
        const routes = data.routes || [];
        const firstRoute = routes[0]?.properties || {};
        const fare = firstRoute.fare?.value ? `${formatNumber(firstRoute.fare.value)}원` : "-";
        const landingUrl = properties.landingURL;

        return routeMessageTemplate(message, "success") + `
            <div class="route-summary-grid">
                <div><span>예상 시간</span><strong>${formatTime(firstRoute.totalTime)}</strong></div>
                <div><span>이동 거리</span><strong>${formatDistance(firstRoute.totalDistance)}</strong></div>
                <div><span>환승</span><strong>${formatNumber(firstRoute.transfers)}회</strong></div>
                <div><span>요금</span><strong>${fare}</strong></div>
            </div>
            ${landingUrl ? `<a class="route-link" href="${escapeAttribute(landingUrl)}" target="_blank" rel="noopener noreferrer">카카오맵에서 자세히 보기</a>` : ""}
        `;
    }

    function renderSimpleRouteSummary(message, data, type) {
        const route = data.route || {};
        const properties = route.properties || {};
        const landingUrl = properties.landingUrl;

        return routeMessageTemplate(message, "success") + `
            <div class="route-summary-grid">
                <div><span>경로 종류</span><strong>${type === "walk" ? "도보" : "자전거"}</strong></div>
                <div><span>예상 시간</span><strong>${formatTime(properties.totalTime)}</strong></div>
                <div><span>이동 거리</span><strong>${formatDistance(properties.totalDistance)}</strong></div>
            </div>
            ${landingUrl ? `<a class="route-link" href="${escapeAttribute(landingUrl)}" target="_blank" rel="noopener noreferrer">카카오맵에서 자세히 보기</a>` : ""}
        `;
    }

    function fitRouteBounds(payload) {
        const bounds = new kakao.maps.LatLngBounds();
        bounds.extend(new kakao.maps.LatLng(Number(payload.startY), Number(payload.startX)));
        bounds.extend(new kakao.maps.LatLng(Number(payload.endY), Number(payload.endX)));
        map.setBounds(bounds);
    }

    function moveToCurrentLocation() {
        if (!navigator.geolocation) {
            showMessage("현재 브라우저에서 위치 기능을 지원하지 않습니다.", "error");
            return;
        }

        showMessage("현재 위치를 확인하는 중입니다.", "");

        navigator.geolocation.getCurrentPosition((position) => {
            const place = {
                id: "current-location",
                name: "현재 위치",
                address: "브라우저에서 확인한 현재 위치입니다.",
                category: "현재 위치",
                x: position.coords.longitude,
                y: position.coords.latitude
            };

            const latLng = new kakao.maps.LatLng(place.y, place.x);
            currentLocationMarker = refreshMarker(currentLocationMarker, latLng, "현재 위치");
            selectedPlace = place;
            map.setLevel(4);
            map.panTo(latLng);
            updateSelectedPlaceUi(place);
            showMessage("현재 위치로 이동했습니다.", "success");
        }, (error) => {
            showMessage(resolveGeoErrorMessage(error), "error");
        }, {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 30000
        });
    }

    function resolveGeoErrorMessage(error) {
        if (error.code === error.PERMISSION_DENIED) {
            return "위치 권한이 거부되었습니다. 브라우저 주소창의 위치 권한을 허용해 주세요.";
        }

        if (error.code === error.POSITION_UNAVAILABLE) {
            return "현재 위치 정보를 가져올 수 없습니다.";
        }

        if (error.code === error.TIMEOUT) {
            return "현재 위치 확인 시간이 초과되었습니다.";
        }

        return "현재 위치 확인 중 오류가 발생했습니다.";
    }

    function changeZoom(delta) {
        if (!map) {
            return;
        }

        const nextLevel = Math.max(1, Math.min(14, map.getLevel() + delta));
        map.setLevel(nextLevel);
    }

    function toggleWalkOption() {
        const routeType = document.querySelector('[data-route-form] [name="type"]')?.value;
        const walkOption = document.querySelector("[data-walk-option]");

        if (walkOption) {
            walkOption.hidden = routeType !== "walk";
        }
    }

    function clearSearchMarkers() {
        searchMarkers.forEach((marker) => marker.setMap(null));
        searchMarkers = [];
    }

    function clearSearchResults(message) {
        clearSearchMarkers();
        const list = document.querySelector("[data-place-result-list]");
        const count = document.querySelector("[data-result-count]");

        if (count) {
            count.textContent = "0개";
        }

        if (list) {
            list.innerHTML = `<div class="empty-result">${escapeHtml(message)}</div>`;
        }
    }

    function showMessage(message, type) {
        const box = document.querySelector("[data-map-message]");
        if (!box) {
            return;
        }

        box.textContent = message;
        box.className = type ? `map-message ${type}` : "map-message";
    }

    function renderRouteMessage(message, type) {
        const result = document.querySelector("[data-route-result]");
        if (result) {
            result.innerHTML = routeMessageTemplate(message, type);
        }
    }

    function routeMessageTemplate(message, type) {
        const className = type ? `route-message ${type}` : "route-message";
        return `<div class="${className}">${escapeHtml(message)}</div>`;
    }

    function setText(selector, value) {
        const element = document.querySelector(selector);
        if (element) {
            element.textContent = value || "";
        }
    }

    function formatDistance(value) {
        const distance = Number(value);
        if (!Number.isFinite(distance)) {
            return "-";
        }

        if (distance >= 1000) {
            return `${(distance / 1000).toFixed(1)}km`;
        }

        return `${formatNumber(distance)}m`;
    }

    function formatTime(value) {
        const seconds = Number(value);
        if (!Number.isFinite(seconds)) {
            return "-";
        }

        const minutes = Math.round(seconds / 60);
        if (minutes >= 60) {
            const hours = Math.floor(minutes / 60);
            const restMinutes = minutes % 60;
            return restMinutes > 0 ? `${hours}시간 ${restMinutes}분` : `${hours}시간`;
        }

        return `${minutes}분`;
    }

    function formatNumber(value) {
        const number = Number(value);
        return Number.isFinite(number) ? number.toLocaleString() : "-";
    }

    function escapeHtml(value) {
        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    function escapeAttribute(value) {
        return escapeHtml(value).replaceAll("`", "&#096;");
    }
})();
