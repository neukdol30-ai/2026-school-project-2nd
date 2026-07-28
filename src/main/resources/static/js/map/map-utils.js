/**
 * 지도 기능에서 공통으로 사용하는 순수 변환·표시 유틸리티입니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};

    function buildRequestUrl(url, params) {
        const queryString = new URLSearchParams();
        Object.entries(params)
            .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== "")
            .forEach(([key, value]) => queryString.append(key, value));
        return queryString.toString() ? `${url}?${queryString}` : url;
    }

    function calculateDistanceBetweenCoordinates(startLat, startLng, endLat, endLng) {
        if (![startLat, startLng, endLat, endLng].every(Number.isFinite)) {
            return 0;
        }

        const earthRadius = 6371000;
        const dLat = toRadian(endLat - startLat);
        const dLng = toRadian(endLng - startLng);
        const a = Math.sin(dLat / 2) ** 2
            + Math.cos(toRadian(startLat)) * Math.cos(toRadian(endLat)) * Math.sin(dLng / 2) ** 2;
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

    namespace.utils = Object.freeze({
        buildRequestUrl,
        calculateDistanceBetweenCoordinates,
        compactCategory,
        formatDistance,
        formatTime,
        formatNumber,
        resolveLocationErrorMessage,
        escapeHtml,
        escapeAttribute
    });
})();
