/**
 * 대중교통 경로 후보와 이동 안내 HTML을 생성합니다.
 * 지도 오버레이 제어와 문자열 렌더링 책임을 분리합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};
    const { formatDistance, formatTime, formatNumber, escapeHtml } = namespace.utils || {};

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

    namespace.routeView = Object.freeze({
        buildPublicRouteCandidateCard,
        buildPublicRouteStepList
    });
})();
