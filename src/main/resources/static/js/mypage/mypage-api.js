/**
 * 마이페이지 조회·변경 요청과 HTTP 응답 정규화를 담당합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMyPage = window.SecondProMyPage || {};
    const { API, LOGIN_URL, ERROR_MESSAGE } = namespace.config || {};

    async function fetchMyPage() {
        try {
            const response = await fetch(API.me, {
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    "Accept": "application/json",
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            if (!response.ok) {
                return null;
            }

            return await readJson(response);
        } catch (error) {
            return null;
        }
    }

    async function postJson(url, payload) {
        try {
            const response = await fetch(url, {
                method: "POST",
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json",
                    "X-Requested-With": "XMLHttpRequest"
                },
                body: JSON.stringify(payload)
            });

            const data = await readJson(response);
            return normalizeActionResponse(response, data);
        } catch (error) {
            return {
                success: false,
                message: ERROR_MESSAGE.network
            };
        }
    }

    function normalizeActionResponse(response, data) {
        if (isSessionExpiredResponse(response, data)) {
            return createSessionExpiredResult(data);
        }

        if (response.status === 403) {
            return {
                success: false,
                message: data && data.message ? data.message : ERROR_MESSAGE.forbidden,
                forbidden: true
            };
        }

        if (response.status === 400) {
            return data || {
                success: false,
                message: ERROR_MESSAGE.badRequest
            };
        }

        if (response.status >= 500) {
            return data || {
                success: false,
                message: ERROR_MESSAGE.server
            };
        }

        if (!response.ok) {
            return data || {
                success: false,
                message: ERROR_MESSAGE.unknown
            };
        }

        if (!isJsonResponse(response)) {
            return {
                success: false,
                message: ERROR_MESSAGE.invalidResponse
            };
        }

        return data || {
            success: false,
            message: ERROR_MESSAGE.invalidResponse
        };
    }

    function isSessionExpiredResponse(response, data) {
        if (response.status === 401) {
            return true;
        }

        if (data && data.sessionExpired) {
            return true;
        }

        if (isLoginPageResponse(response)) {
            return true;
        }

        return response.ok && !isJsonResponse(response) && isHtmlResponse(response);
    }

    function isLoginPageResponse(response) {
        const responseUrl = response && response.url ? response.url : "";
        return response.redirected && responseUrl.includes(LOGIN_URL)
            || responseUrl.includes(LOGIN_URL + "?")
            || responseUrl.endsWith(LOGIN_URL);
    }

    function createSessionExpiredResult(data) {
        return {
            success: false,
            message: data && data.message ? data.message : ERROR_MESSAGE.loginExpired,
            redirectUrl: LOGIN_URL,
            sessionExpired: true
        };
    }

    function isJsonResponse(response) {
        const contentType = response.headers.get("content-type") || "";
        return contentType.toLowerCase().includes("application/json");
    }

    function isHtmlResponse(response) {
        const contentType = response.headers.get("content-type") || "";
        return contentType.toLowerCase().includes("text/html");
    }

    async function readJson(response) {
        if (!isJsonResponse(response)) {
            return null;
        }

        try {
            return await response.json();
        } catch (error) {
            return null;
        }
    }

    namespace.api = Object.freeze({ fetchMyPage, postJson });
})();
