/**
 * 지도 화면의 서버 통신만 담당합니다.
 * 응답 형식과 오류 문구를 화면 코드에서 분리해 API 변경 영향을 줄입니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMap = window.SecondProMap || {};
    const { buildRequestUrl } = namespace.utils || {};

    if (typeof buildRequestUrl !== "function") {
        throw new Error("map-utils.js가 map-api.js보다 먼저 로드되어야 합니다.");
    }

    async function requestFavoriteApi(url, options = {}) {
        const response = await fetch(url, {
            method: options.method || "GET",
            headers: options.headers || {
                "Accept": "application/json"
            },
            body: options.body
        });

        const data = await response.json().catch(() => null);
        if (!response.ok || data?.success === false) {
            throw new Error(data?.message || `즐겨찾기 요청 실패: HTTP ${response.status}`);
        }

        return data;
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

    namespace.api = Object.freeze({ requestFavoriteApi, requestJson });
})();
