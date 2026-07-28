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
    const myPageModules = window.SecondProMyPage;

    if (!myPageModules?.config || !myPageModules?.api || !myPageModules?.utils
        || !myPageModules?.validation || !myPageModules?.renderer) {
        console.error("마이페이지 모듈을 불러오지 못했습니다. mypage 하위 스크립트 로드 순서를 확인해주세요.");
        return;
    }

    const {
        API,
        TAB,
        TAB_ORDER,
        MODAL_ID,
        LOGIN_URL,
        SESSION_REDIRECT_DELAY_MS,
        ERROR_MESSAGE,
        SECURITY_VERIFY_REQUIRED_MESSAGE,
        SECURITY_VERIFY_EXPIRED_MESSAGE
    } = myPageModules.config;
    const { fetchMyPage, postJson } = myPageModules.api;
    const { formToObject, formatPhoneValue } = myPageModules.utils;
    const {
        validateBasicProfilePayload,
        validateSecurityProfilePayload,
        validatePasswordPayload,
        validateWithdrawPayload
    } = myPageModules.validation;

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

    const { renderModalContent } = myPageModules.renderer.createMyPageRenderer(viewState);

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

        if (typeof updateGlobalBanner === "function") {
            updateGlobalBanner();
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

    /**
     * HTTP 상태 코드와 서버 응답 본문을 마이페이지 화면에서 쓰기 쉬운 형태로 통일합니다.
     *
     * 주의
     * - 다른 탭에서 로그아웃된 뒤 저장/변경 요청을 보내면 Spring Security가 401 JSON이 아니라
     *   로그인 HTML 페이지로 redirect 응답을 돌려줄 수 있습니다.
     * - fetch는 기본적으로 redirect를 따라가기 때문에 최종 응답이 200 + text/html 이 될 수 있습니다.
     * - 마이페이지 API는 JSON만 정상 응답으로 보기 때문에, 로그인 페이지 HTML 응답도 세션 만료로 처리합니다.
     */

    /**
     * 세션 만료 여부를 여러 형태로 감지합니다.
     * 1. 서버가 401을 반환한 경우
     * 2. 서버 JSON이 sessionExpired=true를 반환한 경우
     * 3. Spring Security가 로그인 페이지로 redirect한 경우
     * 4. 마이페이지 API 요청인데 JSON이 아닌 HTML 로그인 페이지가 반환된 경우
     */







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





    /* =========================================================
     * 9. 모달 전체 렌더링
     * ======================================================= */


    /* =========================================================
     * 10. 탭 패널 렌더링
     * ======================================================= */








    /* =========================================================
     * 11. 작은 렌더링 유틸
     * ======================================================= */









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













})();
