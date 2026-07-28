/**
 * 폼 변환, 날짜·전화번호 표시, 소셜 제공자 이름과 HTML 이스케이프를 담당합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMyPage = window.SecondProMyPage || {};

    function formToObject(form) {
        const formData = new FormData(form);
        const payload = {};

        formData.forEach((value, key) => {
            if (typeof value !== "string") {
                payload[key] = value;
                return;
            }

            const trimmed = value.trim();
            payload[key] = trimmed === "" ? null : trimmed;
        });

        return payload;
    }

    function formatPhoneValue(value) {
        const digits = String(value || "").replace(/[^0-9]/g, "").slice(0, 11);

        if (digits.length <= 3) {
            return digits;
        }

        if (digits.length <= 7) {
            return `${digits.slice(0, 3)}-${digits.slice(3)}`;
        }

        return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`;
    }

    function todayDateInput() {
        const today = new Date();
        const offsetDate = new Date(today.getTime() - today.getTimezoneOffset() * 60000);
        return offsetDate.toISOString().slice(0, 10);
    }

    function toDateInput(value) {
        if (!value) {
            return "";
        }

        return String(value).substring(0, 10);
    }

    function formatDateTime(value) {
        if (!value) {
            return "기록 없음";
        }

        return String(value).replace("T", " ").substring(0, 16);
    }

    function labelBoardCategory(value) {
        return value === "NOTICE" ? "공지" : "문의";
    }

    function labelAnswerStatus(value) {
        return value === "ANSWERED" ? "답변 완료" : "답변 대기";
    }

    function labelChatStatus(value) {
        return value === "OPEN" ? "진행 중" : "종료";
    }

    function labelChatCategory(value) {
        const labels = {
            MAIL: "메일",
            MAP: "지도",
            STOCK: "증권",
            NEWS: "뉴스",
            WEATHER: "날씨",
            CALENDAR: "캘린더",
            ETC: "기타"
        };

        return labels[value] || "기타";
    }

    function labelGender(value) {
        if (value === "M") {
            return "남성";
        }

        if (value === "F") {
            return "여성";
        }

        return "선택 안 함";
    }

    function getLoginMethodLabel(profile) {
        if (!profile || !profile.socialLoginUser) {
            return "일반 로그인";
        }

        const providers = normalizeProviders(profile.socialProviders);
        if (providers.length === 0) {
            return "소셜 로그인";
        }

        return providers.map(labelSocialProvider).join(", ") + " 계정";
    }

    function hasProvider(profile, provider) {
        return normalizeProviders(profile.socialProviders).includes(provider);
    }

    function normalizeProviders(value) {
        if (!value) {
            return [];
        }

        return String(value)
            .split(",")
            .map((item) => item.trim().toUpperCase())
            .filter(Boolean);
    }

    function labelSocialProvider(provider) {
        const labels = {
            KAKAO: "카카오",
            NAVER: "네이버"
        };
        return labels[provider] || provider;
    }

    function html(value) {
        if (typeof escapeHtml === "function") {
            return escapeHtml(value);
        }

        return String(value ?? "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#039;");
    }

    namespace.utils = Object.freeze({
        formToObject,
        formatPhoneValue,
        todayDateInput,
        toDateInput,
        formatDateTime,
        labelBoardCategory,
        labelAnswerStatus,
        labelChatStatus,
        labelChatCategory,
        labelGender,
        getLoginMethodLabel,
        hasProvider,
        html
    });
})();
