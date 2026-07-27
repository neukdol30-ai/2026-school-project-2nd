/*
 * kakao-dynamic-map.js
 * 5차 패치 역할
 * - 현재 위치 정확도 옵션 개선
 * - 현재 위치 기준 거리순 장소 검색
 * - 검색 결과를 왼쪽 상단 영역에 표시
 * - 검색 결과 전체를 동적지도 마커로 표시
 * - 화면에는 위도/경도 숫자를 노출하지 않음
 */
(() => {
    const ROUTE_API = "/api/kakao-map/route";
    const LOCATION_OPTIONS = {
        enableHighAccuracy: true,
        timeout: 15000,
        maximumAge: 0
    };
    const DEFAULT_MAP_LEVEL = 4;
    const MAX_MAP_LEVEL = 10;
    const MAX_PUBLIC_ROUTE_CANDIDATES = 5;
    const ROUTE_LINE_OPTIONS = {
        strokeWeight: 6,
        strokeColor: "#2563eb",
        strokeOpacity: 0.86,
        strokeStyle: "solid"
    };

    const state = {
        map: null,
        places: null,
        geocoder: null,
        currentPosition: null,
        currentAccuracy: null,
        currentMarker: null,
        resultMarkers: [],
        routePolylines: [],
        routeBounds: null,
        routeMarkers: {
            start: null,
            end: null
        },
        infoWindow: null,
        selectedPlace: null,
        lastBounds: null
    };

    document.addEventListener("DOMContentLoaded", () => {
        if (!window.kakao || !window.kakao.maps) {
            showLocationStatus("카카오 지도 SDK를 불러오지 못했습니다. JavaScript 키와 도메인을 확인해주세요.", "error");
            return;
        }

        window.kakao.maps.load(() => {
            initializeMap();
            bindEvents();
        });
    });

    function initializeMap() {
        const pageConfig = window.KAKAO_MAP_PAGE || {};
        const defaultLat = Number(pageConfig.defaultLat) || 37.566826;
        const defaultLng = Number(pageConfig.defaultLng) || 126.9786567;
        const container = document.getElementById("kakaoDynamicMap");

        if (!container) {
            return;
        }

        const center = new kakao.maps.LatLng(defaultLat, defaultLng);
        state.map = new kakao.maps.Map(container, {
            center,
            level: DEFAULT_MAP_LEVEL
        });
        limitMapZoomOut();
        state.places = new kakao.maps.services.Places(state.map);
        state.geocoder = new kakao.maps.services.Geocoder();
        state.infoWindow = new kakao.maps.InfoWindow({ zIndex: 10 });

        const zoomControl = new kakao.maps.ZoomControl();
        state.map.addControl(zoomControl, kakao.maps.ControlPosition.RIGHT);
        kakao.maps.event.addListener(state.map, "zoom_changed", enforceMaxMapLevel);
        resizeMapAfterLayout();
    }

    function limitMapZoomOut() {
        if (!state.map) {
            return;
        }

        if (typeof state.map.setMaxLevel === "function") {
            state.map.setMaxLevel(MAX_MAP_LEVEL);
        }
    }

    function enforceMaxMapLevel() {
        if (!state.map || typeof state.map.getLevel !== "function") {
            return;
        }

        if (state.map.getLevel() > MAX_MAP_LEVEL) {
            state.map.setLevel(MAX_MAP_LEVEL);
            setMapGuide("지도 축소 범위를 서비스 영역에 맞춰 제한했습니다.");
        }
    }

    function setMapGuide(message) {
        const guide = document.querySelector(".map-toolbar-guide");
        if (guide) {
            guide.textContent = message;
        }
    }

    function bindEvents() {
        const searchForm = document.querySelector("[data-search-form]");
        const currentLocationButton = document.querySelector("[data-current-location-button]");
        const fitResultButton = document.querySelector("[data-fit-result-button]");
        const clearMapButton = document.querySelector("[data-clear-map-button]");
        const routeForm = document.querySelector("[data-route-form]");

        searchForm?.addEventListener("submit", handleSearchSubmit);
        currentLocationButton?.addEventListener("click", moveToCurrentLocation);
        fitResultButton?.addEventListener("click", fitResultBounds);
        clearMapButton?.addEventListener("click", clearMapView);
        routeForm?.addEventListener("submit", handleRouteSubmit);
    }

    async function handleSearchSubmit(event) {
        event.preventDefault();

        const form = event.currentTarget;
        const keyword = form.keyword.value.trim();
        const searchType = form.searchType.value;
        const useCurrentLocation = form.useCurrentLocation.checked;

        if (!keyword) {
            renderEmptyResult("검색어를 입력해주세요.", "장소명이나 주소를 입력하면 결과가 표시됩니다.");
            return;
        }

        setFormLoading(form, true);
        updateResultCount(0);
        renderLoadingResult("검색 중입니다...");

        try {
            if (searchType === "address") {
                searchAddress(keyword);
                return;
            }

            searchKeyword(keyword, useCurrentLocation);
        } finally {
            setFormLoading(form, false);
        }
    }

    function searchKeyword(keyword, useCurrentLocation) {
        const options = {
            size: 15
        };

        const useDistanceSort = useCurrentLocation && isCurrentLocationReliable();

        if (useDistanceSort) {
            options.location = state.currentPosition;
            options.sort = kakao.maps.services.SortBy.DISTANCE;
        } else {
            options.sort = kakao.maps.services.SortBy.ACCURACY;

            if (useCurrentLocation && state.currentPosition && !isCurrentLocationReliable()) {
                showLocationStatus("현재 위치 오차가 커서 정확도순으로 검색합니다. 모바일 GPS 또는 위치 보정 후 다시 시도해주세요.", "warning");
            }
        }

        state.places.keywordSearch(keyword, (data, status) => {
            if (status === kakao.maps.services.Status.OK) {
                const results = normalizePlaceResults(data);
                renderSearchResults(results, options.sort === kakao.maps.services.SortBy.DISTANCE);
                drawSearchMarkers(results);
                return;
            }

            if (status === kakao.maps.services.Status.ZERO_RESULT) {
                clearSearchMarkers();
                renderEmptyResult("검색 결과가 없습니다.", "다른 검색어로 다시 시도해주세요.");
                return;
            }

            renderEmptyResult("장소 검색 중 오류가 발생했습니다.", "잠시 후 다시 시도해주세요.");
        }, options);
    }

    function searchAddress(keyword) {
        state.geocoder.addressSearch(keyword, (data, status) => {
            if (status === kakao.maps.services.Status.OK) {
                const results = normalizeAddressResults(data);
                renderSearchResults(results, false);
                drawSearchMarkers(results);
                return;
            }

            if (status === kakao.maps.services.Status.ZERO_RESULT) {
                clearSearchMarkers();
                renderEmptyResult("주소 검색 결과가 없습니다.", "도로명 또는 지번 주소를 다시 확인해주세요.");
                return;
            }

            renderEmptyResult("주소 검색 중 오류가 발생했습니다.", "잠시 후 다시 시도해주세요.");
        });
    }

    function normalizePlaceResults(data) {
        return data.map((item, index) => {
            const lat = Number(item.y);
            const lng = Number(item.x);
            const fallbackDistance = state.currentPosition ? calculateDistanceFromCurrent(lat, lng) : null;

            return {
                id: item.id || `place-${index}`,
                title: item.place_name || "장소명 없음",
                address: item.road_address_name || item.address_name || "주소 정보 없음",
                category: compactCategory(item.category_name),
                phone: item.phone || "",
                url: item.place_url || "",
                lat,
                lng,
                distance: item.distance ? Number(item.distance) : fallbackDistance,
                source: "place"
            };
        });
    }

    function normalizeAddressResults(data) {
        return data.map((item, index) => ({
            id: `address-${index}`,
            title: item.address_name || "주소 결과",
            address: item.road_address?.address_name || item.address?.address_name || item.address_name || "주소 정보 없음",
            category: "주소",
            phone: "",
            url: "",
            lat: Number(item.y),
            lng: Number(item.x),
            distance: state.currentPosition ? calculateDistanceFromCurrent(Number(item.y), Number(item.x)) : null,
            source: "address"
        }));
    }

    function drawSearchMarkers(results) {
        clearSearchMarkers();

        if (results.length === 0) {
            return;
        }

        const bounds = new kakao.maps.LatLngBounds();

        results.forEach((place, index) => {
            const position = new kakao.maps.LatLng(place.lat, place.lng);
            const marker = new kakao.maps.Marker({
                map: state.map,
                position,
                title: place.title
            });

            kakao.maps.event.addListener(marker, "click", () => {
                selectPlace(place, marker, index + 1);
            });

            marker.__place = place;
            state.resultMarkers.push(marker);
            bounds.extend(position);
        });

        if (state.currentPosition) {
            bounds.extend(state.currentPosition);
        }

        state.lastBounds = bounds;
        resizeMapAfterLayout();
        state.map.setBounds(bounds);
        enforceMaxMapLevel();
        setFitButtonEnabled(true);
        setMapGuide("검색 결과와 마커가 지도에 표시되었습니다.");
    }

    function renderSearchResults(results, distanceSorted) {
        const resultList = document.querySelector("[data-result-list]");

        if (!resultList) {
            return;
        }

        updateResultCount(results.length);

        const sortedLabel = distanceSorted ? "내 위치 기준 가까운 순" : "정확도순";
        const items = results.map((place, index) => `
            <article class="search-result-card" data-result-index="${index}">
                <button type="button" class="result-main-button" data-select-result="${index}">
                    <span class="result-rank">${index + 1}</span>
                    <span class="result-content">
                        <strong>${escapeHtml(place.title)}</strong>
                        <span>${escapeHtml(place.address)}</span>
                        <small>${escapeHtml(place.category || sortedLabel)}${place.distance ? ` · ${formatDistance(place.distance)}` : ""}</small>
                    </span>
                </button>
                <div class="result-card-actions">
                    <button type="button" data-route-point="start" data-result-index="${index}">출발지</button>
                    <button type="button" data-route-point="end" data-result-index="${index}">도착지</button>
                    ${place.url ? `<a href="${escapeAttribute(place.url)}" target="_blank" rel="noopener noreferrer">상세</a>` : ""}
                </div>
            </article>
        `).join("");

        resultList.innerHTML = `
            <div class="result-mode-badge">${escapeHtml(sortedLabel)}</div>
            ${items}
        `;

        resultList.querySelectorAll("[data-select-result]").forEach((button) => {
            button.addEventListener("click", () => {
                const index = Number(button.dataset.selectResult);
                const marker = state.resultMarkers[index];
                selectPlace(results[index], marker, index + 1);
            });
        });

        resultList.querySelectorAll("[data-route-point]").forEach((button) => {
            button.addEventListener("click", () => {
                const index = Number(button.dataset.resultIndex);
                setRoutePoint(button.dataset.routePoint, results[index]);
            });
        });
    }

    function selectPlace(place, marker, markerNumber) {
        state.selectedPlace = place;

        const position = new kakao.maps.LatLng(place.lat, place.lng);
        state.map.panTo(position);

        const infoContent = `
            <div class="map-info-window">
                <strong>${escapeHtml(place.title)}</strong>
                <span>${escapeHtml(place.address)}</span>
                ${place.distance ? `<small>${formatDistance(place.distance)}</small>` : ""}
            </div>
        `;

        if (marker) {
            state.infoWindow.setContent(infoContent);
            state.infoWindow.open(state.map, marker);
        }

        renderSelectedPlace(place, markerNumber);
        highlightResultCard(place);
    }

    function renderSelectedPlace(place, markerNumber) {
        const section = document.querySelector("[data-selected-section]");
        const card = document.querySelector("[data-selected-place-card]");

        if (!section || !card) {
            return;
        }

        section.hidden = false;
        card.innerHTML = `
            <div class="selected-title-row">
                <span class="result-rank large">${markerNumber || "선택"}</span>
                <div>
                    <strong>${escapeHtml(place.title)}</strong>
                    <span>${escapeHtml(place.address)}</span>
                </div>
            </div>
            <div class="selected-meta-row">
                ${place.category ? `<span>${escapeHtml(place.category)}</span>` : ""}
                ${place.distance ? `<span>${formatDistance(place.distance)}</span>` : ""}
                ${place.phone ? `<span>${escapeHtml(place.phone)}</span>` : ""}
            </div>
            <div class="selected-actions">
                <button type="button" data-selected-route="start">출발지로 설정</button>
                <button type="button" data-selected-route="end">도착지로 설정</button>
                <button type="button" data-selected-center>지도 중앙</button>
            </div>
        `;

        card.querySelector('[data-selected-route="start"]')?.addEventListener("click", () => setRoutePoint("start", place));
        card.querySelector('[data-selected-route="end"]')?.addEventListener("click", () => setRoutePoint("end", place));
        card.querySelector("[data-selected-center]")?.addEventListener("click", () => state.map.panTo(new kakao.maps.LatLng(place.lat, place.lng)));
    }

    function highlightResultCard(place) {
        document.querySelectorAll(".search-result-card").forEach((card) => card.classList.remove("active"));
        const index = state.resultMarkers.findIndex((marker) => marker.__place === place);
        const target = document.querySelector(`[data-result-index="${index}"]`);
        target?.classList.add("active");
        target?.scrollIntoView({ behavior: "smooth", block: "nearest" });
    }

    function setRoutePoint(type, place) {
        if (!place) {
            return;
        }

        const form = document.querySelector("[data-route-form]");
        const nameTarget = document.querySelector(type === "start" ? "[data-route-start-name]" : "[data-route-end-name]");

        if (!form || !nameTarget) {
            return;
        }

        const position = new kakao.maps.LatLng(place.lat, place.lng);
        const fieldPrefix = type === "start" ? "start" : "end";
        form[`${fieldPrefix}X`].value = place.lng;
        form[`${fieldPrefix}Y`].value = place.lat;
        form[`${fieldPrefix}Name`].value = place.title;
        nameTarget.textContent = place.title;

        drawRoutePointMarker(type, position, place.title);
        updateRouteBoxState(type);
        fitRoutePointBounds();
        setMapGuide("출발지와 도착지를 선택한 뒤 경로 조회를 누르면 지도에 경로가 표시됩니다.");
    }

    function drawRoutePointMarker(type, position, title) {
        if (state.routeMarkers[type]) {
            state.routeMarkers[type].setMap(null);
        }

        state.routeMarkers[type] = new kakao.maps.Marker({
            map: state.map,
            position,
            title
        });
    }

    function updateRouteBoxState(type) {
        const box = document.querySelector(`[data-route-point-box="${type}"]`);
        box?.classList.add("selected");
    }

    async function handleRouteSubmit(event) {
        event.preventDefault();

        const form = event.currentTarget;
        const result = document.querySelector("[data-route-result]");
        const payload = {
            type: form.type.value,
            routeMode: form.routeMode.value,
            startX: form.startX.value,
            startY: form.startY.value,
            startName: form.startName.value,
            endX: form.endX.value,
            endY: form.endY.value,
            endName: form.endName.value
        };

        if (!payload.startX || !payload.startY || !payload.endX || !payload.endY) {
            result.textContent = "출발지와 도착지를 먼저 선택해주세요.";
            result.classList.add("error");
            return;
        }

        setFormLoading(form, true);
        result.classList.remove("error");
        result.textContent = "경로를 조회하는 중입니다...";

        try {
            const data = await requestJson(ROUTE_API, payload);
            renderRouteSummary(data, payload);
        } catch (error) {
            result.classList.add("error");
            result.textContent = error.message || "경로 조회 중 오류가 발생했습니다.";
        } finally {
            setFormLoading(form, false);
        }
    }

    function renderRouteSummary(response, payload) {
        const result = document.querySelector("[data-route-result]");

        if (!result) {
            return;
        }

        if (!response.success) {
            clearRouteLines();
            result.classList.add("error");
            result.textContent = response.message || "경로 조회 결과가 없습니다.";
            return;
        }

        const data = response.data || {};
        const routeType = payload.type || document.querySelector('[data-route-form] [name="type"]')?.value || "publictraffic";

        if (routeType === "publictraffic") {
            renderPublicRouteSummary(result, data, payload);
            return;
        }

        drawRouteOnMap(data, payload, 0);
        const properties = data.route?.properties || {};
        result.classList.remove("error");
        result.innerHTML = `
            <strong>경로 요약</strong>
            <span>${formatDistance(properties.totalDistance)} · ${formatTime(properties.totalTime)}</span>
            <small>오른쪽 지도에서 경로를 확인할 수 있습니다.</small>
        `;
    }

    function renderPublicRouteSummary(result, data, payload) {
        const routes = Array.isArray(data.routes) ? data.routes : [];

        if (routes.length === 0) {
            clearRouteLines();
            result.classList.add("error");
            result.textContent = "대중교통 경로 조회 결과가 없습니다.";
            return;
        }

        drawRouteOnMap(data, payload, 0);
        result.classList.remove("error");
        updatePublicRouteSummary(result, data, payload, routes, 0);
    }

    function updatePublicRouteSummary(result, data, payload, routes, activeIndex) {
        const selectedRoute = routes[activeIndex] || routes[0];
        const selectedProps = selectedRoute?.properties || {};
        const fareText = selectedProps.fare?.value ? `${formatNumber(selectedProps.fare.value)}원` : "요금 정보 없음";
        const routeCards = routes.slice(0, MAX_PUBLIC_ROUTE_CANDIDATES)
            .map((route, index) => buildPublicRouteCandidateCard(route, index, activeIndex))
            .join("");
        const stepList = buildPublicRouteStepList(selectedRoute);

        result.innerHTML = `
            <strong>경로 요약</strong>
            <span>${formatDistance(selectedProps.totalDistance)} · ${formatTime(selectedProps.totalTime)}</span>
            <small>환승 ${formatNumber(selectedProps.transfers)}회 · ${fareText}</small>
            <small>대중교통은 선택한 한 개 경로만 지도에 표시됩니다.</small>
            <div class="route-selected-guide">
                <strong>이동 안내</strong>
                ${stepList}
            </div>
            <div class="route-candidate-list">${routeCards}</div>
        `;

        result.querySelectorAll("[data-route-candidate]").forEach((button) => {
            button.addEventListener("click", () => {
                const index = Number(button.dataset.routeCandidate);

                drawRouteOnMap(data, payload, index);
                updatePublicRouteSummary(result, data, payload, routes, index);
                setMapGuide(`${index + 1}번 대중교통 경로가 지도에 표시되었습니다.`);
            });
        });
    }

    function buildPublicRouteCandidateCard(route, index, activeIndex) {
        const routeProps = route.properties || {};
        const fareText = routeProps.fare?.value ? `${formatNumber(routeProps.fare.value)}원` : "요금 정보 없음";
        const activeClass = index === activeIndex ? " active" : "";
        const transitOverview = buildPublicTransitOverview(route);

        return `
            <button type="button" class="route-candidate-card${activeClass}" data-route-candidate="${index}">
                <span class="route-candidate-rank">${index + 1}</span>
                <span class="route-candidate-content">
                    <strong>${formatTime(routeProps.totalTime)} · ${formatDistance(routeProps.totalDistance)}</strong>
                    <small>환승 ${formatNumber(routeProps.transfers)}회 · ${fareText}</small>
                    ${transitOverview ? `<small class="route-candidate-guide">${escapeHtml(transitOverview)}</small>` : ""}
                    <em>이 경로 보기</em>
                </span>
            </button>
        `;
    }

    function buildPublicTransitOverview(route) {
        const transitSteps = extractPublicTransitSteps(route)
            .filter((step) => ["BUS", "SUBWAY"].includes(String(step.type || "").toUpperCase()));

        if (transitSteps.length === 0) {
            return "";
        }

        return transitSteps
            .slice(0, 3)
            .map((step) => {
                const vehicleText = formatVehicles(step.vehicles);
                const stopText = formatStopRange(step.stops);

                if (vehicleText && stopText) {
                    return `${vehicleText} · ${stopText}`;
                }

                return vehicleText || step.guidance || resolveRouteStepTypeLabel(step.type);
            })
            .filter(Boolean)
            .join(" → ");
    }

    function buildPublicRouteStepList(route) {
        const steps = extractPublicTransitSteps(route);

        if (steps.length === 0) {
            return `<p class="route-step-empty">상세 이동 안내 정보가 없습니다.</p>`;
        }

        const items = steps
            .slice(0, 10)
            .map((step, index) => {
                const typeLabel = resolveRouteStepTypeLabel(step.type);
                const title = formatRouteStepTitle(step);
                const stopText = formatStopRange(step.stops);
                const meta = [
                    formatDistance(step.distance),
                    formatTime(step.time)
                ].filter(Boolean).join(" · ");

                return `
                    <li class="route-step-item">
                        <span class="route-step-index">${index + 1}</span>
                        <span class="route-step-content">
                            <strong>${escapeHtml(title)}</strong>
                            <small>${escapeHtml(typeLabel)}${meta ? ` · ${escapeHtml(meta)}` : ""}</small>
                            ${stopText ? `<em>${escapeHtml(stopText)}</em>` : ""}
                        </span>
                    </li>
                `;
            })
            .join("");

        return `<ol class="route-step-list">${items}</ol>`;
    }

    function extractPublicTransitSteps(route) {
        const steps = Array.isArray(route?.steps) ? route.steps : [];

        return steps
            .map((step) => step?.properties || {})
            .filter((properties) => {
                const type = String(properties.type || "").toUpperCase();
                return type || properties.guidance || Array.isArray(properties.vehicles) || Array.isArray(properties.stops);
            });
    }

    function formatRouteStepTitle(step) {
        const type = String(step.type || "").toUpperCase();
        const vehicleText = formatVehicles(step.vehicles);

        if (type === "BUS" || type === "SUBWAY") {
            return vehicleText ? `${vehicleText} 탑승` : (step.guidance || resolveRouteStepTypeLabel(step.type));
        }

        return step.guidance || resolveRouteStepTypeLabel(step.type);
    }

    function formatVehicles(vehicles) {
        if (!Array.isArray(vehicles) || vehicles.length === 0) {
            return "";
        }

        return vehicles
            .map((vehicle) => {
                const type = vehicle?.type ? String(vehicle.type).trim() : "";
                const name = vehicle?.name ? String(vehicle.name).trim() : "";

                if (type && name) {
                    return `${type} ${name}`;
                }

                return name || type;
            })
            .filter(Boolean)
            .join(", ");
    }

    function formatStopRange(stops) {
        if (!Array.isArray(stops) || stops.length === 0) {
            return "";
        }

        const firstStop = stops[0]?.name || "";
        const lastStop = stops[stops.length - 1]?.name || "";

        if (firstStop && lastStop && firstStop !== lastStop) {
            return `${firstStop} → ${lastStop}`;
        }

        return firstStop || lastStop;
    }

    function resolveRouteStepTypeLabel(type) {
        const normalizedType = String(type || "").toUpperCase();

        if (normalizedType === "BUS") {
            return "버스";
        }

        if (normalizedType === "SUBWAY") {
            return "지하철";
        }

        if (normalizedType === "WALKING") {
            return "도보";
        }

        return "이동";
    }

    function drawRouteOnMap(routeData, payload, routeIndex = 0) {
        clearRouteLines();

        const startPosition = createLatLngFromPayload(payload.startY, payload.startX);
        const endPosition = createLatLngFromPayload(payload.endY, payload.endX);
        const extractedPath = extractRoutePath(routeData, payload.type, routeIndex);
        const routePath = extractedPath.length >= 2 ? extractedPath : [startPosition, endPosition].filter(Boolean);

        if (routePath.length < 2) {
            setMapGuide("경로 좌표를 지도에 표시하지 못했습니다. 출발지와 도착지 마커만 표시합니다.");
            fitRoutePointBounds();
            return;
        }

        const routeLine = new kakao.maps.Polyline({
            map: state.map,
            path: routePath,
            ...ROUTE_LINE_OPTIONS
        });
        state.routePolylines.push(routeLine);

        const bounds = new kakao.maps.LatLngBounds();
        routePath.forEach((point) => bounds.extend(point));
        if (startPosition) {
            bounds.extend(startPosition);
        }
        if (endPosition) {
            bounds.extend(endPosition);
        }

        state.routeBounds = bounds;
        resizeMapAfterLayout();
        state.map.setBounds(bounds);
        enforceMaxMapLevel();
        setFitButtonEnabled(true);
        setMapGuide(extractedPath.length >= 2
            ? "경로가 오른쪽 지도에 표시되었습니다."
            : "카카오 경로 좌표가 부족해 출발지와 도착지를 직선으로 연결했습니다."
        );
    }

    function createLatLngFromPayload(latValue, lngValue) {
        const lat = Number(latValue);
        const lng = Number(lngValue);

        if (!isValidKoreaCoordinate(lng, lat)) {
            return null;
        }

        return new kakao.maps.LatLng(lat, lng);
    }

    function extractRoutePath(routeData, routeType, routeIndex) {
        const source = resolveRoutePathSource(routeData, routeType, routeIndex);
        const orderedPoints = extractOrderedPathPoints(source);

        if (orderedPoints.length >= 2) {
            return removeDuplicateLatLng(orderedPoints).slice(0, 3000);
        }

        const groups = [];
        collectCoordinateGroups(source, groups);

        const points = [];
        groups.forEach((group) => {
            group.forEach((point) => points.push(point));
        });

        return removeDuplicateLatLng(points).slice(0, 3000);
    }

    function resolveRoutePathSource(routeData, routeType, routeIndex) {
        if (routeType === "publictraffic" && Array.isArray(routeData?.routes)) {
            return routeData.routes[routeIndex] || routeData.routes[0] || routeData;
        }

        if (routeData?.route) {
            return routeData.route;
        }

        return routeData;
    }

    function extractOrderedPathPoints(source) {
        const points = [];
        collectOrderedPathPoints(source, points);
        return points;
    }

    function collectOrderedPathPoints(node, points) {
        if (!node) {
            return;
        }

        if (Array.isArray(node)) {
            node.forEach((item) => collectOrderedPathPoints(item, points));
            return;
        }

        if (typeof node !== "object") {
            return;
        }

        if (node.path) {
            appendPathObjectPoints(node.path, points);
        }

        if (node.geometry) {
            appendPathObjectPoints(node.geometry, points);
        }

        Object.entries(node).forEach(([key, value]) => {
            const lowerKey = key.toLowerCase();

            if (["path", "geometry"].includes(lowerKey)) {
                return;
            }

            if (["sections", "steps", "legs", "roads", "guides", "routes"].includes(lowerKey)) {
                collectOrderedPathPoints(value, points);
            }
        });
    }

    function appendPathObjectPoints(pathObject, points) {
        if (!pathObject) {
            return;
        }

        if (Array.isArray(pathObject)) {
            parseCoordinateArray(pathObject).forEach((point) => points.push(point));
            return;
        }

        if (typeof pathObject !== "object") {
            return;
        }

        ["points", "coordinates", "vertexes", "vertices", "path"].forEach((key) => {
            if (Array.isArray(pathObject[key])) {
                parseCoordinateArray(pathObject[key]).forEach((point) => points.push(point));
            }
        });
    }

    function collectCoordinateGroups(node, groups) {
        if (!node) {
            return;
        }

        if (Array.isArray(node)) {
            const parsed = parseCoordinateArray(node);
            if (parsed.length >= 2) {
                groups.push(parsed);
                return;
            }

            node.forEach((item) => collectCoordinateGroups(item, groups));
            return;
        }

        if (typeof node !== "object") {
            return;
        }

        Object.entries(node).forEach(([key, value]) => {
            const lowerKey = key.toLowerCase();

            if (Array.isArray(value) && ["coordinates", "vertexes", "vertices", "path", "points"].includes(lowerKey)) {
                const parsed = parseCoordinateArray(value);
                if (parsed.length >= 2) {
                    groups.push(parsed);
                    return;
                }
            }

            if (["sections", "steps", "legs", "roads", "guides", "routes", "geometry", "path"].includes(lowerKey)) {
                collectCoordinateGroups(value, groups);
            }
        });
    }

    function parseCoordinateArray(value) {
        if (!Array.isArray(value)) {
            return [];
        }

        if (isFlatNumberArray(value)) {
            return parseFlatCoordinatePairs(value);
        }

        if (isCoordinatePair(value)) {
            return [new kakao.maps.LatLng(Number(value[1]), Number(value[0]))];
        }

        const points = [];
        value.forEach((item) => {
            parseCoordinateArray(item).forEach((point) => points.push(point));
        });
        return points;
    }

    function parseFlatCoordinatePairs(values) {
        const points = [];

        for (let index = 0; index < values.length - 1; index += 2) {
            const lng = Number(values[index]);
            const lat = Number(values[index + 1]);

            if (isValidKoreaCoordinate(lng, lat)) {
                points.push(new kakao.maps.LatLng(lat, lng));
            }
        }

        return points;
    }

    function isFlatNumberArray(value) {
        return Array.isArray(value)
            && value.length >= 4
            && value.every((item) => Number.isFinite(Number(item)));
    }

    function isCoordinatePair(value) {
        if (!Array.isArray(value) || value.length < 2) {
            return false;
        }

        const lng = Number(value[0]);
        const lat = Number(value[1]);
        return isValidKoreaCoordinate(lng, lat);
    }

    function isValidKoreaCoordinate(lng, lat) {
        return Number.isFinite(lng)
            && Number.isFinite(lat)
            && lng >= 123
            && lng <= 132.5
            && lat >= 32
            && lat <= 39.8;
    }

    function removeDuplicateLatLng(points) {
        const result = [];
        const seen = new Set();

        points.forEach((point) => {
            const key = `${point.getLat().toFixed(6)},${point.getLng().toFixed(6)}`;
            if (!seen.has(key)) {
                seen.add(key);
                result.push(point);
            }
        });

        return result;
    }

    function fitRoutePointBounds() {
        const positions = Object.values(state.routeMarkers)
            .filter(Boolean)
            .map((marker) => marker.getPosition());

        if (positions.length === 0) {
            return;
        }

        if (positions.length === 1) {
            state.map.setLevel(DEFAULT_MAP_LEVEL);
            state.map.panTo(positions[0]);
            return;
        }

        const bounds = new kakao.maps.LatLngBounds();
        positions.forEach((position) => bounds.extend(position));
        state.map.setBounds(bounds);
        enforceMaxMapLevel();
    }

    function clearRouteLines() {
        state.routePolylines.forEach((polyline) => polyline.setMap(null));
        state.routePolylines = [];
        state.routeBounds = null;
    }

    function clearRouteOverlays() {
        clearRouteLines();
        Object.entries(state.routeMarkers).forEach(([type, marker]) => {
            if (marker) {
                marker.setMap(null);
                state.routeMarkers[type] = null;
            }
        });
    }

    async function moveToCurrentLocation() {
        const button = document.querySelector("[data-current-location-button]");

        if (!navigator.geolocation) {
            showLocationStatus("이 브라우저에서는 현재 위치 기능을 사용할 수 없습니다.", "error");
            return;
        }

        button.disabled = true;
        button.textContent = "확인 중";
        showLocationStatus("현재 위치를 확인하는 중입니다...", "");

        navigator.geolocation.getCurrentPosition(
            (position) => {
                const { latitude, longitude, accuracy } = position.coords;
                const latLng = new kakao.maps.LatLng(latitude, longitude);

                state.currentPosition = latLng;
                state.currentAccuracy = accuracy;
                drawCurrentLocationMarker(latLng);
                state.map.setLevel(3);
                state.map.panTo(latLng);

                const accuracyMessage = accuracy > 5000
                    ? `현재 위치를 찾았지만 오차가 큽니다. 약 ${formatDistance(accuracy)} 범위입니다. 거리순 검색은 정확도순으로 보정됩니다.`
                    : `현재 위치가 적용되었습니다. 약 ${formatDistance(accuracy)} 범위입니다.`;

                showLocationStatus(accuracyMessage, accuracy > 5000 ? "warning" : "success");
                button.disabled = false;
                button.textContent = "현재 위치";
            },
            (error) => {
                showLocationStatus(resolveLocationErrorMessage(error), "error");
                button.disabled = false;
                button.textContent = "현재 위치";
            },
            LOCATION_OPTIONS
        );
    }

    function drawCurrentLocationMarker(position) {
        if (state.currentMarker) {
            state.currentMarker.setMap(null);
        }

        state.currentMarker = new kakao.maps.Marker({
            map: state.map,
            position,
            title: "현재 위치"
        });
    }

    function fitResultBounds() {
        const bounds = state.routeBounds || state.lastBounds;

        if (bounds) {
            resizeMapAfterLayout();
            state.map.setBounds(bounds);
            enforceMaxMapLevel();
        }
    }

    function isCurrentLocationReliable() {
        return Boolean(state.currentPosition) && (!state.currentAccuracy || state.currentAccuracy <= 5000);
    }

    function resizeMapAfterLayout() {
        if (!state.map || !window.kakao?.maps?.event) {
            return;
        }

        window.setTimeout(() => {
            kakao.maps.event.trigger(state.map, "resize");
        }, 80);
    }

    function clearMapView() {
        clearSearchMarkers();
        clearRouteOverlays();
        state.infoWindow?.close();
        state.selectedPlace = null;
        state.lastBounds = null;
        setFitButtonEnabled(false);
        updateResultCount(0);
        renderEmptyResult("검색 결과가 초기화되었습니다.", "다시 검색하면 결과와 마커가 표시됩니다.");
        document.querySelector("[data-selected-section]")?.setAttribute("hidden", "hidden");
        resetRoutePanel();
        setMapGuide("검색 결과와 선택 장소가 지도에 표시됩니다.");
    }

    function resetRoutePanel() {
        const form = document.querySelector("[data-route-form]");
        const result = document.querySelector("[data-route-result]");

        if (form) {
            form.reset();
            ["startX", "startY", "startName", "endX", "endY", "endName"].forEach((name) => {
                if (form[name]) {
                    form[name].value = "";
                }
            });
        }

        const startName = document.querySelector("[data-route-start-name]");
        const endName = document.querySelector("[data-route-end-name]");
        if (startName) {
            startName.textContent = "선택 안 됨";
        }
        if (endName) {
            endName.textContent = "선택 안 됨";
        }
        document.querySelectorAll(".route-point-box").forEach((box) => box.classList.remove("selected"));

        if (result) {
            result.classList.remove("error");
            result.textContent = "출발지와 도착지를 선택하면 경로 요약을 확인할 수 있습니다.";
        }
    }

    function clearSearchMarkers() {
        state.resultMarkers.forEach((marker) => marker.setMap(null));
        state.resultMarkers = [];
    }

    function renderLoadingResult(message) {
        const resultList = document.querySelector("[data-result-list]");
        if (resultList) {
            resultList.innerHTML = `<div class="empty-state loading"><strong>${escapeHtml(message)}</strong></div>`;
        }
    }

    function renderEmptyResult(title, description) {
        const resultList = document.querySelector("[data-result-list]");
        if (!resultList) {
            return;
        }

        updateResultCount(0);
        resultList.innerHTML = `
            <div class="empty-state">
                <strong>${escapeHtml(title)}</strong>
                <p>${escapeHtml(description || "")}</p>
            </div>
        `;
    }

    function updateResultCount(count) {
        const target = document.querySelector("[data-result-count]");
        if (target) {
            target.textContent = `${Number(count || 0).toLocaleString()}개`;
        }
    }

    function setFitButtonEnabled(enabled) {
        const button = document.querySelector("[data-fit-result-button]");
        if (button) {
            button.disabled = !enabled;
        }
    }

    function showLocationStatus(message, type) {
        const target = document.querySelector("[data-location-status]");
        if (!target) {
            return;
        }

        target.textContent = message;
        target.classList.remove("success", "error", "warning");
        if (type) {
            target.classList.add(type);
        }
    }

    async function requestJson(url, params) {
        const requestUrl = buildRequestUrl(url, params);
        const response = await fetch(requestUrl, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            }
        });

        if (!response.ok) {
            throw new Error(`서버 요청 실패: HTTP ${response.status}`);
        }

        return response.json();
    }

    function buildRequestUrl(url, params) {
        const queryString = new URLSearchParams();
        Object.entries(params)
            .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
            .forEach(([key, value]) => queryString.append(key, value));
        return queryString.toString() ? `${url}?${queryString}` : url;
    }

    function setFormLoading(form, loading) {
        form.querySelectorAll("button, input, select").forEach((element) => {
            element.disabled = loading;
        });
    }

    function calculateDistanceFromCurrent(lat, lng) {
        if (!state.currentPosition || !Number.isFinite(lat) || !Number.isFinite(lng)) {
            return null;
        }

        const currentLat = state.currentPosition.getLat();
        const currentLng = state.currentPosition.getLng();
        const earthRadius = 6371000;
        const dLat = toRadian(lat - currentLat);
        const dLng = toRadian(lng - currentLng);
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(toRadian(currentLat)) * Math.cos(toRadian(lat)) * Math.sin(dLng / 2) ** 2;
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    function toRadian(value) {
        return value * Math.PI / 180;
    }

    function compactCategory(categoryName) {
        if (!categoryName) {
            return "";
        }

        const parts = String(categoryName).split(">").map((item) => item.trim()).filter(Boolean);
        return parts.at(-1) || categoryName;
    }

    function formatDistance(value) {
        const distance = Number(value);
        if (!Number.isFinite(distance)) {
            return "";
        }

        if (distance >= 1000) {
            return `${(distance / 1000).toFixed(1)}km`;
        }

        return `${Math.round(distance).toLocaleString()}m`;
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

    function resolveLocationErrorMessage(error) {
        if (!error) {
            return "현재 위치를 가져오지 못했습니다.";
        }

        if (error.code === error.PERMISSION_DENIED) {
            return "위치 권한이 거부되었습니다. 브라우저 주소창의 위치 권한을 허용해주세요.";
        }

        if (error.code === error.POSITION_UNAVAILABLE) {
            return "현재 위치 정보를 사용할 수 없습니다. Wi-Fi 또는 모바일 위치 설정을 확인해주세요.";
        }

        if (error.code === error.TIMEOUT) {
            return "현재 위치 확인 시간이 초과되었습니다. 다시 시도해주세요.";
        }

        return "현재 위치를 가져오지 못했습니다.";
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
