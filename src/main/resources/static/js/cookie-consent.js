/*
 * cookie-consent.js
 * 역할: 쿠키 동의 배너 표시, 개인화/분석 쿠키 저장, 쿠키 설정 모달 처리
 * 인증 쿠키는 직접 만들지 않고 Spring Security 세션 쿠키를 사용합니다.
 */
(function () {
    "use strict";

    var COOKIE_MAX_AGE = 60 * 60 * 24 * 365;
    var CONSENT_COOKIE = "SECONDPRO_COOKIE_CONSENT";
    var PERSONALIZATION_COOKIE = "SECONDPRO_PERSONALIZATION";
    var ANALYTICS_COOKIE = "SECONDPRO_ANALYTICS";
    var LAST_CHAT_VIEW_COOKIE = "SECONDPRO_LAST_CHAT_VIEW";

    function setCookie(name, value, maxAge) {
        document.cookie = encodeURIComponent(name) + "=" + encodeURIComponent(value)
            + "; path=/"
            + "; max-age=" + (maxAge || COOKIE_MAX_AGE)
            + "; SameSite=Lax";
    }

    function getCookie(name) {
        var target = encodeURIComponent(name) + "=";
        var cookies = document.cookie ? document.cookie.split(";") : [];
        for (var i = 0; i < cookies.length; i++) {
            var cookie = cookies[i].trim();
            if (cookie.indexOf(target) === 0) {
                return decodeURIComponent(cookie.substring(target.length));
            }
        }
        return "";
    }

    function hasConsent() { return getCookie(CONSENT_COOKIE) !== ""; }
    function isPersonalizationAllowed() { return getCookie(PERSONALIZATION_COOKIE) === "Y"; }

    function saveChatViewPreference() {
        if (!isPersonalizationAllowed()) return;
        var path = window.location.pathname;
        if (path.indexOf("/chat/mobile") === 0) {
            setCookie(LAST_CHAT_VIEW_COOKIE, "mobile");
        } else if (path.indexOf("/chat") === 0) {
            setCookie(LAST_CHAT_VIEW_COOKIE, "pc");
        }
    }

    function createBanner() {
        if (document.getElementById("cookieBanner")) return;
        var banner = document.createElement("section");
        banner.id = "cookieBanner";
        banner.className = "cookie-banner";
        banner.setAttribute("aria-label", "쿠키 동의 안내");
        banner.innerHTML =
            '<div class="cookie-banner__content">' +
            '  <h2 class="cookie-banner__title">쿠키 사용 안내</h2>' +
            '  <p class="cookie-banner__text">로그인 상태 유지를 위해 필수 세션 쿠키를 사용합니다. 개인화 쿠키는 화면 선호 저장에, 분석 쿠키는 방문 기록 통계에 사용됩니다.</p>' +
            '</div>' +
            '<div class="cookie-banner__actions">' +
            '  <button type="button" class="cookie-button cookie-button--primary" data-cookie-accept-all>모두 허용</button>' +
            '  <button type="button" class="cookie-button cookie-button--outline" data-cookie-essential>필수만 허용</button>' +
            '  <button type="button" class="cookie-button cookie-button--light" data-cookie-manage>설정</button>' +
            '</div>';
        document.body.appendChild(banner);
    }

    function createModal() {
        if (document.getElementById("cookieModal")) return;
        var modal = document.createElement("section");
        modal.id = "cookieModal";
        modal.className = "cookie-modal";
        modal.setAttribute("aria-label", "쿠키 설정");
        modal.innerHTML =
            '<div class="cookie-modal__panel">' +
            '  <div class="cookie-modal__header">' +
            '    <div><h2 class="cookie-modal__title">쿠키 설정</h2><p class="cookie-modal__desc">필수 쿠키는 로그인과 보안을 위해 항상 사용됩니다. 개인화/분석 쿠키는 선택할 수 있습니다.</p></div>' +
            '    <button type="button" class="cookie-modal__close" data-cookie-close aria-label="닫기">×</button>' +
            '  </div>' +
            '  <div class="cookie-option"><div><h3 class="cookie-option__name">필수 쿠키</h3><p class="cookie-option__desc">Spring Security의 세션 쿠키처럼 로그인 상태 유지와 접근 권한 확인에 필요한 쿠키입니다.</p></div><label class="cookie-switch"><input type="checkbox" checked disabled><span class="cookie-switch__slider"></span></label></div>' +
            '  <div class="cookie-option"><div><h3 class="cookie-option__name">개인화 쿠키</h3><p class="cookie-option__desc">PC/모바일 채팅 화면 선호 같은 사용자 설정을 저장합니다.</p></div><label class="cookie-switch"><input type="checkbox" id="cookiePersonalization"><span class="cookie-switch__slider"></span></label></div>' +
            '  <div class="cookie-option"><div><h3 class="cookie-option__name">분석 쿠키</h3><p class="cookie-option__desc">방문 페이지와 접속 시간을 기록해 기능 사용 통계를 확인합니다.</p></div><label class="cookie-switch"><input type="checkbox" id="cookieAnalytics"><span class="cookie-switch__slider"></span></label></div>' +
            '  <div class="cookie-modal__actions"><button type="button" class="cookie-button cookie-button--ghost" data-cookie-essential>필수만 허용</button><button type="button" class="cookie-button cookie-button--dark" data-cookie-save>선택 저장</button><button type="button" class="cookie-button cookie-button--primary" data-cookie-accept-all>모두 허용</button></div>' +
            '</div>';
        document.body.appendChild(modal);
    }

    function createFloatingButton() {
        if (document.getElementById("cookieFloatingButton")) return;
        var button = document.createElement("button");
        button.id = "cookieFloatingButton";
        button.type = "button";
        button.className = "cookie-floating-button";
        button.textContent = "쿠키 설정";
        button.setAttribute("data-cookie-manage", "");
        document.body.appendChild(button);
    }

    function showBannerIfNeeded() {
        var banner = document.getElementById("cookieBanner");
        var button = document.getElementById("cookieFloatingButton");
        if (!banner || !button) return;
        if (!hasConsent()) {
            banner.classList.add("is-visible");
            button.classList.remove("is-visible");
        } else {
            banner.classList.remove("is-visible");
            button.classList.add("is-visible");
        }
    }

    function openModal() {
        var modal = document.getElementById("cookieModal");
        var personalization = document.getElementById("cookiePersonalization");
        var analytics = document.getElementById("cookieAnalytics");
        if (personalization) personalization.checked = getCookie(PERSONALIZATION_COOKIE) === "Y";
        if (analytics) analytics.checked = getCookie(ANALYTICS_COOKIE) === "Y";
        if (modal) modal.classList.add("is-visible");
    }

    function closeModal() {
        var modal = document.getElementById("cookieModal");
        if (modal) modal.classList.remove("is-visible");
    }

    function acceptAll() {
        setCookie(CONSENT_COOKIE, "accepted");
        setCookie(PERSONALIZATION_COOKIE, "Y");
        setCookie(ANALYTICS_COOKIE, "Y");
        saveChatViewPreference();
        closeModal();
        showBannerIfNeeded();
    }

    function essentialOnly() {
        setCookie(CONSENT_COOKIE, "essential");
        setCookie(PERSONALIZATION_COOKIE, "N");
        setCookie(ANALYTICS_COOKIE, "N");
        closeModal();
        showBannerIfNeeded();
    }

    function saveCustomSelection() {
        var personalization = document.getElementById("cookiePersonalization");
        var analytics = document.getElementById("cookieAnalytics");
        setCookie(CONSENT_COOKIE, "accepted");
        setCookie(PERSONALIZATION_COOKIE, personalization && personalization.checked ? "Y" : "N");
        setCookie(ANALYTICS_COOKIE, analytics && analytics.checked ? "Y" : "N");
        saveChatViewPreference();
        closeModal();
        showBannerIfNeeded();
    }

    function bindEvents() {
        document.addEventListener("click", function (event) {
            if (event.target.closest("[data-cookie-accept-all]")) { acceptAll(); return; }
            if (event.target.closest("[data-cookie-essential]")) { essentialOnly(); return; }
            if (event.target.closest("[data-cookie-manage]")) { openModal(); return; }
            if (event.target.closest("[data-cookie-save]")) { saveCustomSelection(); return; }
            if (event.target.closest("[data-cookie-close]")) { closeModal(); return; }
            if (event.target.id === "cookieModal") { closeModal(); }
        });
    }

    function init() {
        createBanner();
        createModal();
        createFloatingButton();
        bindEvents();
        saveChatViewPreference();
        showBannerIfNeeded();
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", init);
    } else {
        init();
    }
})();
