/**
 * SecondPro 마이페이지 모달 스크립트
 * ------------------------------------------------------------
 * 담당 역할
 * 1. 메인 계정 위젯의 로그인 상태 동기화
 * 2. 마이페이지 모달 열기/닫기
 * 3. 탭 전환 및 각 탭 화면 렌더링
 * 4. 내 정보, 개인정보, 비밀번호, 회원 탈퇴 요청 처리
 * 5. 전화번호/날짜/비밀번호/탈퇴 확인 문구 1차 검증
 *
 * 주의사항
 * - 실제 보안 검증은 서버에서도 반드시 수행되어야 합니다.*/
(function () {
    "use strict";

    /* =========================================================
     * 1. 상수
     * ======================================================= */
    const API = Object.freeze({
        me: "/mypage/me",
        profile: "/mypage/profile",
        verifyPassword: "/mypage/verify-password",
        security: "/mypage/security",
        password: "/mypage/password",
        withdraw: "/mypage/withdraw"
    });

    const TAB = Object.freeze({
        profile: "profile",
        security: "security",
        password: "password",
        social: "social",
        activity: "activity",
        withdraw: "withdraw"
    });

    /**
     * 탭 순서를 한 곳에서 관리합니다.
     * 마우스 클릭뿐 아니라 키보드 방향키 이동에서도 같은 순서를 사용합니다.
     */
    const TAB_ORDER = Object.freeze([
        TAB.profile,
        TAB.security,
        TAB.password,
        TAB.social,
        TAB.activity,
        TAB.withdraw
    ]);

    const MODAL_ID = "myPageModal";
    const LOGIN_URL = "/member/login";
    const SESSION_REDIRECT_DELAY_MS = 900;
    const PHONE_PATTERN = /^010-[0-9]{4}-[0-9]{4}$/;
    const WITHDRAW_CONFIRM_TEXT = "회원탈퇴";
    const SECURITY_VERIFY_REQUIRED_MESSAGE = "개인정보 수정을 위해 현재 비밀번호 인증이 필요합니다.";
    const SECURITY_VERIFY_EXPIRED_MESSAGE = "본인 확인 시간이 만료되었습니다. 다시 현재 비밀번호를 인증해 주세요.";

    const ERROR_MESSAGE = Object.freeze({
        loginExpired: "로그인이 만료되었습니다. 다시 로그인해 주세요.",
        forbidden: "접근 권한이 없습니다.",
        badRequest: "요청 정보가 올바르지 않습니다.",
        server: "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
        network: "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
        invalidResponse: "응답 형식이 올바르지 않습니다.",
        unknown: "처리 결과를 확인할 수 없습니다."
    });

    /**
     * 모달 내부 화면 상태입니다.
     * 서버 데이터가 아니라 UI 전환 상태만 보관합니다.
     */
    const viewState = {
        initialized: false,
        profileEditMode: false,
        securityEditMode: false,
        securityUnlocked: false,
        passwordChangeMode: false,
        submitLocked: false,
        lastFocusedElement: null
    };

    /* =========================================================
     * 2. 초기화
     * ======================================================= */
    window.initializeMyPage = function initializeMyPage() {
        if (viewState.initialized) {
            return;
        }

        viewState.initialized = true;
        bindMyPageEvents();
        loadCurrentLoginUser();
    };

    function startMyPageWhenReady() {
        if (typeof state === "undefined" || typeof renderAuthWidget !== "function") {
            setTimeout(startMyPageWhenReady, 30);
            return;
        }

        window.initializeMyPage();
    }

    startMyPageWhenReady();

    /* =========================================================
     * 3. 로그인 상태 조회 및 메인 계정 위젯 동기화
     * ======================================================= */
    async function loadCurrentLoginUser() {
        const data = await fetchMyPage();

        if (!data || !data.loggedIn || !data.profile) {
            clearCurrentUser();
            return;
        }

        applyMyPageData(data);
        safeUpdateAuthWidget();
    }

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

    function applyMyPageData(data) {
        state.currentUser = data.profile;
        state.myPage = data;
    }

    function clearCurrentUser() {
        state.currentUser = null;
        state.myPage = null;
        safeUpdateAuthWidget();
    }

    function safeUpdateAuthWidget() {
        if (typeof updateAuthWidget === "function") {
            updateAuthWidget();
        }
    }

    /* =========================================================
     * 4. 이벤트 바인딩
     * ======================================================= */
    function bindMyPageEvents() {
        document.addEventListener("click", handleMyPageClick);
        document.addEventListener("input", handleMyPageInput);
        document.addEventListener("submit", handleMyPageSubmit);
        document.addEventListener("keydown", handleMyPageKeydown);
    }

    function handleMyPageClick(event) {
        const openButton = event.target.closest("[data-mypage-open]");
        if (openButton) {
            event.preventDefault();
            openMyPageModal();
            return;
        }

        const closeButton = event.target.closest("[data-mypage-close]");
        if (closeButton) {
            event.preventDefault();
            closeMyPageModal();
            return;
        }

        const tabButton = event.target.closest("[data-mypage-tab]");
        if (tabButton) {
            event.preventDefault();
            activateMyPageTab(tabButton.dataset.mypageTab);
            return;
        }

        const editProfileButton = event.target.closest("[data-mypage-profile-edit]");
        if (editProfileButton) {
            event.preventDefault();
            viewState.profileEditMode = true;
            renderCurrentMyPage(TAB.profile);
            return;
        }

        const cancelProfileButton = event.target.closest("[data-mypage-profile-cancel]");
        if (cancelProfileButton) {
            event.preventDefault();
            viewState.profileEditMode = false;
            renderCurrentMyPage(TAB.profile);
            return;
        }

        const editSecurityButton = event.target.closest("[data-mypage-security-edit]");
        if (editSecurityButton) {
            event.preventDefault();
            viewState.securityEditMode = true;
            renderCurrentMyPage(TAB.security);
            return;
        }

        const cancelSecurityButton = event.target.closest("[data-mypage-security-cancel]");
        if (cancelSecurityButton) {
            event.preventDefault();
            viewState.securityEditMode = false;
            renderCurrentMyPage(TAB.security);
            return;
        }

        const editPasswordButton = event.target.closest("[data-mypage-password-edit]");
        if (editPasswordButton) {
            event.preventDefault();
            viewState.passwordChangeMode = true;
            renderCurrentMyPage(TAB.password);
            return;
        }

        const cancelPasswordButton = event.target.closest("[data-mypage-password-cancel]");
        if (cancelPasswordButton) {
            event.preventDefault();
            viewState.passwordChangeMode = false;
            renderCurrentMyPage(TAB.password);
            return;
        }

        const passwordToggle = event.target.closest("[data-mypage-password-toggle]");
        if (passwordToggle) {
            event.preventDefault();
            togglePasswordVisibility(passwordToggle);
            return;
        }

        closeWhenOverlayClicked(event);
    }

    function handleMyPageInput(event) {
        const phoneInput = event.target.closest("[data-mypage-phone]");
        if (phoneInput) {
            phoneInput.value = formatPhoneValue(phoneInput.value);
        }
    }

    function handleMyPageSubmit(event) {
        const form = event.target;

        if (form.matches("#myPageProfileForm")) {
            event.preventDefault();
            runSubmitWithLock(form, () => submitProfileForm(form));
            return;
        }

        if (form.matches("#myPageSecurityVerifyForm")) {
            event.preventDefault();
            runSubmitWithLock(form, () => submitSecurityVerifyForm(form));
            return;
        }

        if (form.matches("#myPageSecurityProfileForm")) {
            event.preventDefault();
            runSubmitWithLock(form, () => submitSecurityProfileForm(form));
            return;
        }

        if (form.matches("#myPagePasswordForm")) {
            event.preventDefault();
            runSubmitWithLock(form, () => submitPasswordForm(form));
            return;
        }

        if (form.matches("#myPageWithdrawForm")) {
            event.preventDefault();
            runSubmitWithLock(form, () => submitWithdrawForm(form));
        }
    }

    function handleMyPageKeydown(event) {
        const modal = document.getElementById(MODAL_ID);
        if (!modal) {
            return;
        }

        if (handleMyPageTabKeydown(event)) {
            return;
        }

        if (event.key === "Escape") {
            event.preventDefault();
            closeMyPageModal();
            return;
        }

        if (event.key === "Tab") {
            keepFocusInsideModal(event);
        }
    }

    function closeWhenOverlayClicked(event) {
        const modal = document.getElementById(MODAL_ID);
        if (modal && event.target === modal) {
            closeMyPageModal();
        }
    }

    /* =========================================================
     * 5. 모달 열기/닫기/탭 전환
     * ======================================================= */
    async function openMyPageModal() {
        rememberFocusedElement();

        const data = await fetchMyPage();

        if (!data || !data.loggedIn || !data.profile) {
            location.href = LOGIN_URL;
            return;
        }

        applyMyPageData(data);
        resetModalViewModes(data.profile);
        safeUpdateAuthWidget();
        renderMyPageModal(data, TAB.profile);
    }

    function resetModalViewModes(profile) {
        viewState.profileEditMode = false;
        viewState.securityEditMode = false;
        viewState.securityUnlocked = Boolean(profile && profile.socialLoginUser);
        viewState.passwordChangeMode = false;
        viewState.submitLocked = false;
    }

    /**
     * 비밀번호 변경 성공 후에는 서버의 보안 인증 세션이 제거됩니다.
     * 화면 상태도 함께 잠가야 다음에 보안 설정 탭으로 이동했을 때 재인증 화면이 표시됩니다.
     */
    function resetSecurityVerificationView(profile) {
        viewState.securityEditMode = false;
        viewState.securityUnlocked = Boolean(profile && profile.socialLoginUser);
    }

    function renderCurrentMyPage(activeTab) {
        if (!state.myPage) {
            return;
        }

        renderMyPageModal(state.myPage, activeTab || getActiveTabName() || TAB.profile);
    }

    function renderMyPageModal(data, activeTab) {
        removeExistingModal();

        const modal = document.createElement("div");
        modal.id = MODAL_ID;
        modal.className = "mypage-overlay";
        modal.innerHTML = renderModalContent(data, normalizeTabName(activeTab));
        document.body.appendChild(modal);
        document.body.classList.add("mypage-open");
        focusMyPageDialog();
    }

    function removeExistingModal() {
        const existingModal = document.getElementById(MODAL_ID);
        if (existingModal) {
            existingModal.remove();
        }
    }

    function closeMyPageModal() {
        removeExistingModal();
        document.body.classList.remove("mypage-open");
        restoreRememberedFocus();
    }

    /**
     * 모달을 닫았을 때 원래 눌렀던 계정 버튼으로 초점을 되돌리기 위해 저장합니다.
     */
    function rememberFocusedElement() {
        viewState.lastFocusedElement = document.activeElement instanceof HTMLElement
            ? document.activeElement
            : null;
    }

    /**
     * 모달이 열린 직후 키보드 사용자가 바로 모달 안에서 이동할 수 있게 합니다.
     */
    function focusMyPageDialog() {
        const dialog = document.querySelector(`#${MODAL_ID} .mypage-modal`);
        if (!dialog) {
            return;
        }

        dialog.focus({ preventScroll: true });
    }

    /**
     * 모달을 닫은 후 포커스를 이전 위치로 복원합니다.
     */
    function restoreRememberedFocus() {
        const target = viewState.lastFocusedElement;
        viewState.lastFocusedElement = null;

        if (target && document.contains(target) && typeof target.focus === "function") {
            target.focus({ preventScroll: true });
        }
    }

    /**
     * Tab 키가 모달 밖으로 빠져나가지 않도록 막습니다.
     */
    function keepFocusInsideModal(event) {
        const dialog = document.querySelector(`#${MODAL_ID} .mypage-modal`);
        if (!dialog) {
            return;
        }

        const focusableElements = Array.from(dialog.querySelectorAll(
            'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter((element) => element.offsetParent !== null);

        if (focusableElements.length === 0) {
            event.preventDefault();
            dialog.focus({ preventScroll: true });
            return;
        }

        const first = focusableElements[0];
        const last = focusableElements[focusableElements.length - 1];

        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
            return;
        }

        if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    /**
     * 탭 버튼에 초점이 있을 때 방향키/Home/End로 탭을 이동할 수 있게 합니다.
     * 브라우저 기본 탭 이동(Tab 키)은 그대로 두고, 탭 목록 내부 이동만 보강합니다.
     */
    function handleMyPageTabKeydown(event) {
        const tabButton = event.target.closest("[data-mypage-tab]");
        if (!tabButton) {
            return false;
        }

        const key = event.key;
        if (!["ArrowLeft", "ArrowRight", "Home", "End"].includes(key)) {
            return false;
        }

        const buttons = Array.from(document.querySelectorAll(`#${MODAL_ID} [data-mypage-tab]`));
        if (buttons.length === 0) {
            return false;
        }

        const currentIndex = Math.max(0, buttons.indexOf(tabButton));
        let nextIndex = currentIndex;

        if (key === "ArrowRight") {
            nextIndex = (currentIndex + 1) % buttons.length;
        }

        if (key === "ArrowLeft") {
            nextIndex = (currentIndex - 1 + buttons.length) % buttons.length;
        }

        if (key === "Home") {
            nextIndex = 0;
        }

        if (key === "End") {
            nextIndex = buttons.length - 1;
        }

        event.preventDefault();
        const nextButton = buttons[nextIndex];
        activateMyPageTab(nextButton.dataset.mypageTab);
        nextButton.focus({ preventScroll: true });
        return true;
    }

    function activateMyPageTab(tabName) {
        const modal = document.getElementById(MODAL_ID);
        const safeTabName = normalizeTabName(tabName);
        if (!modal) {
            return;
        }

        modal.querySelectorAll("[data-mypage-tab]").forEach((button) => {
            const active = button.dataset.mypageTab === safeTabName;
            button.classList.toggle("active", active);
            button.setAttribute("aria-selected", String(active));
            button.setAttribute("tabindex", active ? "0" : "-1");
        });

        modal.querySelectorAll("[data-mypage-panel]").forEach((panel) => {
            const active = panel.dataset.mypagePanel === safeTabName;
            panel.classList.toggle("active", active);
            panel.setAttribute("aria-hidden", String(!active));
        });
    }

    function getActiveTabName() {
        const active = document.querySelector("[data-mypage-tab].active");
        return active ? active.dataset.mypageTab : null;
    }

    /**
     * 알 수 없는 탭 이름이 들어와도 첫 화면으로 안전하게 돌립니다.
     */
    function normalizeTabName(tabName) {
        return TAB_ORDER.includes(tabName) ? tabName : TAB.profile;
    }

    /* =========================================================
     * 6. Form submit 처리
     * ======================================================= */
    async function submitProfileForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateBasicProfilePayload(payload);
        if (clientMessage) {
            showError(clientMessage);
            return;
        }

        const result = await postJson(API.profile, payload);
        if (!result.success || !result.myPage) {
            showMyPageMessage(result);
            return;
        }

        applyMyPageData(result.myPage);
        viewState.profileEditMode = false;
        safeUpdateAuthWidget();
        renderMyPageModal(result.myPage, TAB.profile);
        showMyPageMessage(result);
    }

    async function submitSecurityVerifyForm(form) {
        const payload = formToObject(form);
        if (!payload.currentPassword) {
            showError("현재 비밀번호를 입력해 주세요.");
            return;
        }

        const result = await postJson(API.verifyPassword, payload);
        if (!result.success || !result.myPage) {
            showMyPageMessage(result);
            return;
        }

        applyMyPageData(result.myPage);
        viewState.securityUnlocked = true;
        viewState.securityEditMode = false;
        viewState.passwordChangeMode = false;
        renderMyPageModal(result.myPage, TAB.security);
        showMyPageMessage(result);
    }

    async function submitSecurityProfileForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateSecurityProfilePayload(payload);
        if (clientMessage) {
            showError(clientMessage);
            return;
        }

        const result = await postJson(API.security, payload);
        if (!result.success || !result.myPage) {
            handleSecurityProfileFailure(result);
            return;
        }

        applyMyPageData(result.myPage);
        viewState.securityEditMode = false;
        safeUpdateAuthWidget();
        renderMyPageModal(result.myPage, TAB.security);
        showMyPageMessage(result);
    }

    function handleSecurityProfileFailure(result) {
        if (isSecurityVerificationFailure(result) && state.myPage) {
            viewState.securityUnlocked = false;
            viewState.securityEditMode = false;
            renderMyPageModal(state.myPage, TAB.security);
        }

        showMyPageMessage(result);
    }

    function isSecurityVerificationFailure(result) {
        const message = result && result.message;
        return message === SECURITY_VERIFY_REQUIRED_MESSAGE
                || message === SECURITY_VERIFY_EXPIRED_MESSAGE;
    }

    async function submitPasswordForm(form) {
        const payload = formToObject(form);
        const clientMessage = validatePasswordPayload(payload);
        if (clientMessage) {
            showError(clientMessage);
            return;
        }

        const result = await postJson(API.password, payload);
        if (!result.success) {
            showMyPageMessage(result);
            return;
        }

        const latestMyPage = result.myPage || state.myPage;
        if (latestMyPage) {
            applyMyPageData(latestMyPage);
        }

        viewState.passwordChangeMode = false;
        resetSecurityVerificationView(latestMyPage ? latestMyPage.profile : null);

        if (latestMyPage) {
            renderMyPageModal(latestMyPage, TAB.password);
            showMyPageMessage(result);
            return;
        }

        form.reset();
        showMyPageMessage(result);
    }

    async function submitWithdrawForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateWithdrawPayload(payload);
        if (clientMessage) {
            showError(clientMessage);
            return;
        }

        if (!confirm("회원 탈퇴를 진행하시겠습니까? 탈퇴 후에는 계정 복구가 어렵습니다.")) {
            return;
        }

        const result = await postJson(API.withdraw, payload);
        showMyPageMessage(result);

        if (result.success) {
            alert(result.message || "회원 탈퇴가 완료되었습니다.");
            location.href = result.redirectUrl || "/";
        }
    }

    /* =========================================================
     * 7. API / Form 유틸
     * ======================================================= */
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

    /**
     * HTTP 상태 코드와 서버 응답 본문을 마이페이지 화면에서 쓰기 쉬운 형태로 통일합니다.
     *
     * 주의
     * - 다른 탭에서 로그아웃된 뒤 저장/변경 요청을 보내면 Spring Security가 401 JSON이 아니라
     *   로그인 HTML 페이지로 redirect 응답을 돌려줄 수 있습니다.
     * - fetch는 기본적으로 redirect를 따라가기 때문에 최종 응답이 200 + text/html 이 될 수 있습니다.
     * - 마이페이지 API는 JSON만 정상 응답으로 보기 때문에, 로그인 페이지 HTML 응답도 세션 만료로 처리합니다.
     */
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

    /**
     * 세션 만료 여부를 여러 형태로 감지합니다.
     * 1. 서버가 401을 반환한 경우
     * 2. 서버 JSON이 sessionExpired=true를 반환한 경우
     * 3. Spring Security가 로그인 페이지로 redirect한 경우
     * 4. 마이페이지 API 요청인데 JSON이 아닌 HTML 로그인 페이지가 반환된 경우
     */
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

    /**
     * 동일한 폼이 빠르게 여러 번 제출되어 중복 요청이 발생하는 것을 방지합니다.
     */
    async function runSubmitWithLock(form, submitTask) {
        if (viewState.submitLocked) {
            return;
        }

        viewState.submitLocked = true;
        setFormBusy(form, true);

        try {
            await submitTask();
        } finally {
            viewState.submitLocked = false;
            setFormBusy(form, false);
        }
    }

    /**
     * 요청 처리 중에는 폼 내부 버튼을 잠시 비활성화합니다.
     */
    function setFormBusy(form, busy) {
        if (!form) {
            return;
        }

        form.setAttribute("aria-busy", String(busy));
        form.querySelectorAll("button").forEach((button) => {
            button.disabled = busy;
        });
    }

    /* =========================================================
     * 8. 검증 / 메시지
     * ======================================================= */
    function showError(message) {
        showMyPageMessage({ success: false, message });
    }

    function showMyPageMessage(result) {
        const safeResult = result || {
            success: false,
            message: ERROR_MESSAGE.unknown
        };

        const messageBox = document.querySelector("[data-mypage-message]");
        if (!messageBox) {
            handleSessionExpiredRedirect(safeResult);
            return;
        }

        messageBox.textContent = safeResult.message || "";
        messageBox.className = "mypage-message " + (safeResult.success ? "success" : "error");
        messageBox.hidden = false;
        messageBox.scrollIntoView({ block: "nearest", behavior: "smooth" });

        handleSessionExpiredRedirect(safeResult);
    }

    /**
     * 세션 만료 결과라면 현재 사용자 정보를 비우고 로그인 화면으로 이동합니다.
     * 메시지 영역이 없는 예외 상황에서도 redirect가 누락되지 않도록 별도 함수로 분리했습니다.
     */
    function handleSessionExpiredRedirect(result) {
        if (!result || !result.sessionExpired || !result.redirectUrl) {
            return;
        }

        clearCurrentUser();
        redirectAfterMessage(result.redirectUrl);
    }

    /**
     * 세션 만료 안내 메시지를 사용자가 볼 수 있도록 짧게 기다린 뒤 로그인 화면으로 이동합니다.
     */
    function redirectAfterMessage(url) {
        window.setTimeout(() => {
            location.href = url;
        }, SESSION_REDIRECT_DELAY_MS);
    }

    function validateBasicProfilePayload(payload) {
        if (!payload.name) {
            return "이름을 입력해 주세요.";
        }

        if (!payload.nickname) {
            return "닉네임을 입력해 주세요.";
        }

        if (payload.nickname.length < 2 || payload.nickname.length > 10) {
            return "닉네임은 2~10자로 입력해 주세요.";
        }

        return null;
    }

    function validateSecurityProfilePayload(payload) {
        if (payload.phone && !PHONE_PATTERN.test(payload.phone)) {
            return "전화번호는 010-0000-0000 형식으로 입력해 주세요.";
        }

        if (payload.birthDate && payload.birthDate > todayDateInput()) {
            return "생년월일은 오늘 이후 날짜로 설정할 수 없습니다.";
        }

        return null;
    }

    function validatePasswordPayload(payload) {
        if (!payload.currentPassword || !payload.newPassword || !payload.newPasswordCheck) {
            return "현재 비밀번호, 새 비밀번호, 새 비밀번호 확인을 모두 입력해 주세요.";
        }

        if (payload.newPassword !== payload.newPasswordCheck) {
            return "새 비밀번호 확인이 일치하지 않습니다.";
        }

        if (payload.currentPassword === payload.newPassword) {
            return "현재 사용 중인 비밀번호와 같은 비밀번호로는 변경할 수 없습니다.";
        }

        return null;
    }

    function validateWithdrawPayload(payload) {
        if (payload.confirmText !== WITHDRAW_CONFIRM_TEXT) {
            return "회원 탈퇴를 진행하려면 확인 문구에 '회원탈퇴'를 정확히 입력해 주세요.";
        }

        return null;
    }

    /* =========================================================
     * 9. 모달 전체 렌더링
     * ======================================================= */
    function renderModalContent(data, activeTab) {
        const profile = data.profile;
        const activity = data.activity || {};
        const recentBoards = data.recentBoards || [];
        const recentChats = data.recentChats || [];
        const isAdmin = String(profile.role || "").toUpperCase() === "ADMIN";
        const loginMethodLabel = getLoginMethodLabel(profile);
        const safeActiveTab = normalizeTabName(activeTab);

        return `
            <section class="mypage-modal" role="dialog" aria-modal="true" aria-labelledby="myPageTitle" tabindex="-1">
                <button class="mypage-close" type="button" data-mypage-close aria-label="마이페이지 닫기">×</button>

                <header class="mypage-profile-header">
                    <div class="mypage-avatar-wrap">
                        <div class="mypage-avatar-text">${html(profile.avatarText || "U")}</div>
                    </div>
                    <div class="mypage-profile-main">
                        <h2 id="myPageTitle">${html(profile.displayName || profile.memberId)}님의 마이페이지</h2>
                        <p>${html(profile.memberId || "")}</p>
                        <span>${html(loginMethodLabel)} · 최근 로그인 ${formatDateTime(profile.lastLoginDate)}</span>
                    </div>
                </header>

                <nav class="mypage-tabs" role="tablist" aria-label="마이페이지 메뉴">
                    ${renderTab(TAB.profile, "👤", "내 정보", safeActiveTab)}
                    ${renderTab(TAB.security, "🔒", "보안 설정", safeActiveTab)}
                    ${renderTab(TAB.password, "🔑", "비밀번호 변경", safeActiveTab)}
                    ${renderTab(TAB.social, "🔗", "소셜 연결", safeActiveTab)}
                    ${renderTab(TAB.activity, "💬", "내 활동", safeActiveTab)}
                    ${renderTab(TAB.withdraw, "⚠️", "회원 탈퇴", safeActiveTab)}
                </nav>

                <div class="mypage-message" data-mypage-message role="status" aria-live="polite" hidden></div>

                <div class="mypage-panels">
                    <section id="myPagePanel-${TAB.profile}" class="mypage-panel ${safeActiveTab === TAB.profile ? "active" : ""}" data-mypage-panel="${TAB.profile}" role="tabpanel" aria-labelledby="myPageTab-${TAB.profile}" aria-hidden="${safeActiveTab !== TAB.profile}" tabindex="0">
                        ${renderProfilePanel(profile)}
                    </section>

                    <section id="myPagePanel-${TAB.security}" class="mypage-panel ${safeActiveTab === TAB.security ? "active" : ""}" data-mypage-panel="${TAB.security}" role="tabpanel" aria-labelledby="myPageTab-${TAB.security}" aria-hidden="${safeActiveTab !== TAB.security}" tabindex="0">
                        ${renderSecurityPanel(profile)}
                    </section>

                    <section id="myPagePanel-${TAB.password}" class="mypage-panel ${safeActiveTab === TAB.password ? "active" : ""}" data-mypage-panel="${TAB.password}" role="tabpanel" aria-labelledby="myPageTab-${TAB.password}" aria-hidden="${safeActiveTab !== TAB.password}" tabindex="0">
                        ${renderPasswordPanel(profile)}
                    </section>

                    <section id="myPagePanel-${TAB.social}" class="mypage-panel ${safeActiveTab === TAB.social ? "active" : ""}" data-mypage-panel="${TAB.social}" role="tabpanel" aria-labelledby="myPageTab-${TAB.social}" aria-hidden="${safeActiveTab !== TAB.social}" tabindex="0">
                        ${renderSocialPanel(profile)}
                    </section>

                    <section id="myPagePanel-${TAB.activity}" class="mypage-panel ${safeActiveTab === TAB.activity ? "active" : ""}" data-mypage-panel="${TAB.activity}" role="tabpanel" aria-labelledby="myPageTab-${TAB.activity}" aria-hidden="${safeActiveTab !== TAB.activity}" tabindex="0">
                        ${renderActivityPanel(activity, recentBoards, recentChats)}
                    </section>

                    <section id="myPagePanel-${TAB.withdraw}" class="mypage-panel ${safeActiveTab === TAB.withdraw ? "active" : ""}" data-mypage-panel="${TAB.withdraw}" role="tabpanel" aria-labelledby="myPageTab-${TAB.withdraw}" aria-hidden="${safeActiveTab !== TAB.withdraw}" tabindex="0">
                        ${renderWithdrawPanel(isAdmin, profile)}
                    </section>
                </div>
            </section>
        `;
    }

    function renderTab(tabName, icon, label, activeTab) {
        const active = activeTab === tabName;
        return `
            <button id="myPageTab-${tabName}"
                    type="button"
                    role="tab"
                    class="mypage-tab ${active ? "active" : ""}"
                    data-mypage-tab="${tabName}"
                    aria-selected="${active}"
                    aria-controls="myPagePanel-${tabName}"
                    tabindex="${active ? "0" : "-1"}">
                <span class="mypage-tab-icon" aria-hidden="true">${icon}</span>
                <span>${label}</span>
            </button>
        `;
    }

    /* =========================================================
     * 10. 탭 패널 렌더링
     * ======================================================= */
    function renderProfilePanel(profile) {
        if (!viewState.profileEditMode) {
            return `
                <div class="mypage-read-card">
                    <div class="mypage-read-grid">
                        ${readItem("아이디", profile.memberId)}
                        ${readItem("이름", profile.name)}
                        ${readItem("닉네임", profile.nickname)}
                        ${readItem("로그인 방식", getLoginMethodLabel(profile))}
                    </div>
                    <div class="mypage-form-footer">
                        <button type="button" class="mypage-primary-btn" data-mypage-profile-edit>수정 모드</button>
                        <button type="button" class="mypage-secondary-btn" data-mypage-close>닫기</button>
                    </div>
                </div>
            `;
        }

        return `
            <form id="myPageProfileForm" class="mypage-form">
                <div class="mypage-form-grid">
                    ${readonlyInput("아이디", profile.memberId)}
                    ${input("이름", "name", profile.name, true)}
                    ${input("닉네임", "nickname", profile.nickname, true)}
                </div>
                <div class="mypage-form-footer">
                    <button type="button" class="mypage-secondary-btn" data-mypage-profile-cancel>취소</button>
                    <button type="submit" class="mypage-primary-btn">저장</button>
                </div>
            </form>
        `;
    }

    function renderSecurityPanel(profile) {
        const isSocialUser = Boolean(profile.socialLoginUser);
        if (isSocialUser) {
            viewState.securityUnlocked = true;
        }

        if (!viewState.securityUnlocked) {
            return `
                <div class="mypage-lock-layout">
                    <div class="mypage-info-box">
                        <h3>개인정보 보호</h3>
                        <p>이메일, 전화번호, 생년월일, 주소는 현재 비밀번호 인증 후 확인하고 수정할 수 있습니다.</p>
                    </div>
                    <form id="myPageSecurityVerifyForm" class="mypage-form mypage-narrow-form">
                        ${passwordInput("현재 비밀번호", "currentPassword", true, "current-password")}
                        <div class="mypage-form-footer">
                            <button type="submit" class="mypage-primary-btn">본인 확인</button>
                        </div>
                    </form>
                </div>
            `;
        }

        return renderSecurityProfileForm(profile);
    }

    function renderSecurityProfileForm(profile) {
        if (!viewState.securityEditMode) {
            return `
                <section class="mypage-security-section">
                    <div class="mypage-section-head">
                        <div>
                            <h3>개인정보</h3>
                            <p>수정이 필요할 때만 수정 모드로 전환해 주세요.</p>
                        </div>
                        <button type="button" class="mypage-primary-btn" data-mypage-security-edit>수정 모드</button>
                    </div>
                    <div class="mypage-read-grid">
                        ${readItem("이메일", profile.email)}
                        ${readItem("전화번호", profile.phone)}
                        ${readItem("생년월일", toDateInput(profile.birthDate))}
                        ${readItem("성별", labelGender(profile.gender))}
                        ${readItem("우편번호", profile.postcode)}
                        ${readItem("주소", profile.address)}
                        ${readItem("상세주소", profile.detailAddress)}
                    </div>
                </section>
            `;
        }

        return `
            <section class="mypage-security-section">
                <div class="mypage-section-head">
                    <div>
                        <h3>개인정보 수정</h3>
                        <p>이메일, 전화번호, 생년월일, 주소를 수정할 수 있습니다.</p>
                    </div>
                </div>
                <form id="myPageSecurityProfileForm" class="mypage-form">
                    <div class="mypage-form-grid">
                        ${input("이메일", "email", profile.email, false, "email")}
                        ${input("전화번호", "phone", profile.phone, false, "tel", "010-0000-0000", "data-mypage-phone inputmode=\"numeric\" maxlength=\"13\"")}
                        ${input("생년월일", "birthDate", toDateInput(profile.birthDate), false, "date", "", `max=\"${todayDateInput()}\"`)}
                        ${selectGender(profile.gender)}
                        ${addressInputs(profile)}
                    </div>
                    <div class="mypage-form-footer">
                        <button type="button" class="mypage-secondary-btn" data-mypage-security-cancel>취소</button>
                        <button type="submit" class="mypage-primary-btn">개인정보 저장</button>
                    </div>
                </form>
            </section>
        `;
    }

    function renderPasswordPanel(profile) {
        if (profile.socialLoginUser) {
            return `
                <section class="mypage-password-page">
                    <div class="mypage-password-page-head">
                        <h3>비밀번호 변경</h3>
                        <p>소셜 로그인 계정은 사이트에서 비밀번호를 직접 관리하지 않습니다.</p>
                    </div>
                    <div class="mypage-setting-row disabled">
                        <div class="mypage-setting-info">
                            <span class="mypage-setting-icon">🔐</span>
                            <div>
                                <strong>비밀번호</strong>
                                <p>${html(getLoginMethodLabel(profile))} 계정은 사이트에서 비밀번호를 관리하지 않습니다.</p>
                            </div>
                        </div>
                        <span class="mypage-status-pill">변경 불가</span>
                    </div>
                    <div class="mypage-info-box mypage-social-password-box compact">
                        <h3>소셜 계정 비밀번호 안내</h3>
                        <p>비밀번호 변경은 카카오/네이버 계정 설정에서 진행해 주세요.</p>
                    </div>
                </section>
            `;
        }

        if (!viewState.passwordChangeMode) {
            return `
                <section class="mypage-password-page">
                    <div class="mypage-password-page-head">
                        <h3>비밀번호 변경</h3>
                        <p>현재 비밀번호 확인 후 새 비밀번호로 변경할 수 있습니다.</p>
                    </div>
                    <div class="mypage-setting-row">
                        <div class="mypage-setting-info">
                            <span class="mypage-setting-icon">🔐</span>
                            <div>
                                <strong>비밀번호</strong>
                                <p>계정 보호를 위해 주기적으로 비밀번호를 변경해 주세요.</p>
                            </div>
                        </div>
                        <button type="button" class="mypage-secondary-btn mypage-setting-action" data-mypage-password-edit>비밀번호 변경</button>
                    </div>
                </section>
            `;
        }

        return `
            <section class="mypage-password-page">
                <div class="mypage-password-change-layout">
                    <div class="mypage-password-change-header">
                        <div>
                            <h3>비밀번호 변경</h3>
                            <p>안전한 비밀번호로 내 정보를 보호하세요.</p>
                        </div>
                        <button type="button" class="mypage-secondary-btn" data-mypage-password-cancel>취소</button>
                    </div>
                    <ul class="mypage-password-rules">
                        <li>다른 사이트에서 사용하지 않은 비밀번호를 권장합니다.</li>
                        <li>이전에 사용한 적 없는 비밀번호가 안전합니다.</li>
                        <li>대문자, 소문자, 숫자를 포함한 8~20자로 입력해 주세요.</li>
                    </ul>
                    <form id="myPagePasswordForm" class="mypage-form mypage-password-form">
                        <div class="mypage-password-card">
                            <div class="mypage-password-field-list">
                                ${passwordInput("현재 비밀번호", "currentPassword", true, "current-password")}
                                ${passwordInput("새 비밀번호", "newPassword", true, "new-password")}
                                ${passwordInput("새 비밀번호 확인", "newPasswordCheck", true, "new-password")}
                            </div>
                            <div class="mypage-form-footer mypage-password-footer">
                                <button type="submit" class="mypage-primary-btn">확인</button>
                                <button type="button" class="mypage-secondary-btn" data-mypage-password-cancel>취소</button>
                            </div>
                        </div>
                    </form>
                </div>
            </section>
        `;
    }

    function renderSocialPanel(profile) {
        const kakaoConnected = Boolean(profile.kakaoConnected) || hasProvider(profile, "KAKAO");
        const naverConnected = Boolean(profile.naverConnected) || hasProvider(profile, "NAVER");

        return `
            <section class="mypage-social-page">
                <div class="mypage-social-page-head">
                    <h3>소셜 계정 연결</h3>
                    <p>카카오와 네이버 계정을 연결하면 다음 로그인부터 소셜 계정으로 간편하게 접속할 수 있습니다.</p>
                </div>

                <div class="mypage-social-list">
                    ${socialProviderCard("kakao", "카카오", kakaoConnected, "카카오 계정으로 로그인할 수 있습니다.")}
                    ${socialProviderCard("naver", "네이버", naverConnected, "네이버 계정으로 로그인할 수 있습니다.")}
                </div>

                <div class="mypage-info-box mypage-social-guide-box">
                    <h3>소셜 연결 안내</h3>
                    <p>현재 화면은 연결 상태 확인용입니다. 실제 연결/해제는 OAuth2 정책과 예외처리 확정 후 별도 API로 확장하는 것을 권장합니다.</p>
                    <p>실서비스에서는 이미 다른 회원에게 연결된 소셜 계정 차단, 마지막 로그인 수단 보호, 재인증 처리를 함께 적용합니다.</p>
                </div>
            </section>
        `;
    }

    function socialProviderCard(provider, label, connected, description) {
        return `
            <article class="mypage-social-card ${connected ? "connected" : ""}">
                <div class="mypage-social-provider-main">
                    <span class="mypage-social-provider-icon ${provider}" aria-hidden="true"></span>
                    <div class="mypage-social-provider-text">
                        <strong>${label}</strong>
                        <p>${description}</p>
                    </div>
                </div>
                <div class="mypage-social-provider-action">
                    <span class="mypage-social-status ${connected ? "connected" : "waiting"}">${connected ? "연결됨" : "연결 안 됨"}</span>
                    <button type="button" class="mypage-secondary-btn" disabled>${connected ? "연결 해제 준비 중" : "연결 준비 중"}</button>
                </div>
            </article>
        `;
    }

    function renderActivityPanel(activity, recentBoards, recentChats) {
        return `
            <div class="mypage-activity-grid">
                ${activityCard("작성 게시글", activity.boardCount || 0)}
                ${activityCard("답변 대기 문의", activity.waitingQuestionCount || 0)}
                ${activityCard("1:1 상담 내역", activity.chatCount || 0)}
                ${activityCard("진행 중 상담", activity.openChatCount || 0)}
            </div>

            <div class="mypage-section-grid">
                <section class="mypage-list-section">
                    <div class="mypage-section-title">
                        <h3>최근 게시글</h3>
                        <a href="/board/list">전체 보기</a>
                    </div>
                    ${renderRecentBoards(recentBoards)}
                </section>

                <section class="mypage-list-section">
                    <div class="mypage-section-title">
                        <h3>1:1 채팅 상담하기</h3>
                        <div class="mypage-section-links">
                            <a href="/chat">상담 시작</a>
                            <a href="/chat/history?from=mypage">상담 내역</a>
                        </div>
                    </div>
                    ${renderRecentChats(recentChats)}
                </section>
            </div>
        `;
    }

    function renderWithdrawPanel(isAdmin, profile) {
        if (isAdmin) {
            return `
                <div class="mypage-danger-box">
                    <h3>관리자 계정 보호</h3>
                    <p>관리자 계정은 마이페이지에서 탈퇴할 수 없습니다. 권한과 계정 삭제는 관리자 콘솔에서 관리해 주세요.</p>
                </div>
            `;
        }

        const isSocialUser = Boolean(profile.socialLoginUser);

        return `
            <form id="myPageWithdrawForm" class="mypage-form mypage-narrow-form">
                <div class="mypage-danger-box">
                    <h3>회원 탈퇴</h3>
                    <p>탈퇴 후 계정 정보와 일부 이용 기록이 삭제되거나 작성자 없음으로 처리될 수 있습니다.</p>
                    ${isSocialUser ? `
                        <p>현재 계정은 ${html(getLoginMethodLabel(profile))} 계정이므로 사이트 비밀번호 입력 없이 현재 로그인 세션과 확인 문구로 탈퇴를 진행합니다.</p>
                    ` : ""}
                </div>
                ${isSocialUser ? "" : passwordInput("현재 비밀번호", "password", true, "current-password")}
                ${input("확인 문구", "confirmText", "", true, "text", "회원탈퇴")}
                <label class="mypage-field mypage-full-field">
                    <span>탈퇴 사유</span>
                    <textarea name="reason" rows="3" placeholder="선택 입력"></textarea>
                </label>
                <div class="mypage-form-footer">
                    <button type="submit" class="mypage-danger-btn">회원 탈퇴</button>
                </div>
            </form>
        `;
    }

    /* =========================================================
     * 11. 작은 렌더링 유틸
     * ======================================================= */
    function renderRecentBoards(items) {
        if (items.length === 0) {
            return `<p class="mypage-empty">최근 작성한 게시글이 없습니다.</p>`;
        }

        return `
            <ul class="mypage-simple-list">
                ${items.map((item) => `
                    <li>
                        <a href="/board/detail/${item.no}">${html(item.title)}</a>
                        <span>${labelBoardCategory(item.category)} · ${labelAnswerStatus(item.answerStatus)}</span>
                    </li>
                `).join("")}
            </ul>
        `;
    }

    function renderRecentChats(items) {
        if (items.length === 0) {
            return `<p class="mypage-empty">최근 상담 내역이 없습니다.</p>`;
        }

        return `
            <ul class="mypage-simple-list">
                ${items.map((item) => `
                    <li>
                        <a href="/chat/room/${item.roomNo}">상담 #${html(item.roomNo)}</a>
                        <span>${labelChatCategory(item.category)} · ${labelChatStatus(item.status)}</span>
                        <small>${html(item.lastMessage || "최근 메시지 없음")}</small>
                    </li>
                `).join("")}
            </ul>
        `;
    }

    function activityCard(label, value) {
        return `
            <div class="mypage-activity-card">
                <span>${label}</span>
                <strong>${html(value)}</strong>
            </div>
        `;
    }

    function input(label, name, value, required, type, placeholder, extraAttributes) {
        return `
            <label class="mypage-field">
                <span>${label}</span>
                <input type="${type || "text"}"
                       name="${name}"
                       value="${html(value || "")}"
                       ${placeholder ? `placeholder="${html(placeholder)}"` : ""}
                       ${extraAttributes || ""}
                       ${required ? "required" : ""}>
            </label>
        `;
    }

    function readonlyInput(label, value) {
        return `
            <label class="mypage-field">
                <span>${label}</span>
                <input type="text" value="${html(value || "-")}" readonly>
            </label>
        `;
    }

    function readItem(label, value) {
        return `
            <div class="mypage-read-item">
                <span>${label}</span>
                <strong>${html(value || "-")}</strong>
            </div>
        `;
    }

    function addressInputs(profile) {
        return `
            <label class="mypage-field">
                <span>우편번호</span>
                <div class="mypage-address-row">
                    <input id="mypagePostcode" type="text" name="postcode" value="${html(profile.postcode || "")}" readonly>
                    <button type="button"
                            class="mypage-secondary-btn"
                            data-address-search
                            data-postcode-target="#mypagePostcode"
                            data-address-target="#mypageAddress"
                            data-detail-target="#mypageDetailAddress">주소 검색</button>
                </div>
            </label>
            <label class="mypage-field mypage-full-field">
                <span>주소</span>
                <input id="mypageAddress" type="text" name="address" value="${html(profile.address || "")}" readonly>
            </label>
            <label class="mypage-field mypage-full-field">
                <span>상세주소</span>
                <input id="mypageDetailAddress" type="text" name="detailAddress" value="${html(profile.detailAddress || "")}" placeholder="상세주소를 입력해 주세요.">
            </label>
        `;
    }

    function passwordInput(label, name, required, autocomplete) {
        return `
            <label class="mypage-field">
                <span>${label}</span>
                <div class="mypage-password-control">
                    <input type="password"
                           name="${name}"
                           autocomplete="${autocomplete || "new-password"}"
                           ${required ? "required" : ""}>
                    <button type="button"
                            class="mypage-password-toggle"
                            data-mypage-password-toggle
                            aria-label="비밀번호 표시">보기</button>
                </div>
            </label>
        `;
    }

    function selectGender(value) {
        const current = value || "";
        return `
            <label class="mypage-field">
                <span>성별</span>
                <select name="gender">
                    <option value="" ${current === "" ? "selected" : ""}>선택 안 함</option>
                    <option value="M" ${current === "M" ? "selected" : ""}>남성</option>
                    <option value="F" ${current === "F" ? "selected" : ""}>여성</option>
                </select>
            </label>
        `;
    }

    function togglePasswordVisibility(button) {
        const control = button.closest(".mypage-password-control");
        if (!control) {
            return;
        }

        const input = control.querySelector("input");
        if (!input) {
            return;
        }

        const shouldShow = input.type === "password";
        input.type = shouldShow ? "text" : "password";
        button.textContent = shouldShow ? "숨김" : "보기";
        button.setAttribute("aria-label", shouldShow ? "비밀번호 숨기기" : "비밀번호 표시");
    }

    /* =========================================================
     * 12. 표시값 변환 유틸
     * ======================================================= */
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
})();
