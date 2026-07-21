(function () {
    const API = {
        me: "/mypage/me",
        profile: "/mypage/profile",
        verifyPassword: "/mypage/verify-password",
        security: "/mypage/security",
        password: "/mypage/password",
        withdraw: "/mypage/withdraw"
    };

    const MODAL_ID = "myPageModal";
    const PHONE_PATTERN = /^010-[0-9]{4}-[0-9]{4}$/;
    const WITHDRAW_CONFIRM_TEXT = "회원탈퇴";

    let initialized = false;
    let profileEditMode = false;
    let securityEditMode = false;
    let securityUnlocked = false;
    let passwordChangeMode = false;

    window.initializeMyPage = function initializeMyPage() {
        if (initialized) {
            return;
        }

        initialized = true;
        bindMyPageEvents();
        loadCurrentLoginUser();
    };

    async function loadCurrentLoginUser() {
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
                clearCurrentUser();
                return;
            }

            const data = await readJson(response);
            if (!data || !data.loggedIn || !data.profile) {
                clearCurrentUser();
                return;
            }

            state.currentUser = data.profile;
            state.myPage = data;
            safeUpdateAuthWidget();
        } catch (error) {
            clearCurrentUser();
        }
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

    function bindMyPageEvents() {
        document.addEventListener("click", function (event) {
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
                profileEditMode = true;
                renderCurrentMyPage("profile");
                return;
            }

            const cancelProfileButton = event.target.closest("[data-mypage-profile-cancel]");
            if (cancelProfileButton) {
                event.preventDefault();
                profileEditMode = false;
                renderCurrentMyPage("profile");
                return;
            }

            const editSecurityButton = event.target.closest("[data-mypage-security-edit]");
            if (editSecurityButton) {
                event.preventDefault();
                securityEditMode = true;
                renderCurrentMyPage("security");
                return;
            }

            const cancelSecurityButton = event.target.closest("[data-mypage-security-cancel]");
            if (cancelSecurityButton) {
                event.preventDefault();
                securityEditMode = false;
                renderCurrentMyPage("security");
                return;
            }

            const editPasswordButton = event.target.closest("[data-mypage-password-edit]");
            if (editPasswordButton) {
                event.preventDefault();
                passwordChangeMode = true;
                renderCurrentMyPage("password");
                return;
            }

            const cancelPasswordButton = event.target.closest("[data-mypage-password-cancel]");
            if (cancelPasswordButton) {
                event.preventDefault();
                passwordChangeMode = false;
                renderCurrentMyPage("password");
                return;
            }

            const passwordToggle = event.target.closest("[data-mypage-password-toggle]");
            if (passwordToggle) {
                event.preventDefault();
                togglePasswordVisibility(passwordToggle);
                return;
            }

            const modal = document.getElementById(MODAL_ID);
            if (modal && event.target === modal) {
                closeMyPageModal();
            }
        });

        document.addEventListener("input", function (event) {
            const phoneInput = event.target.closest("[data-mypage-phone]");
            if (phoneInput) {
                phoneInput.value = formatPhoneValue(phoneInput.value);
            }
        });

        document.addEventListener("submit", function (event) {
            const profileForm = event.target.closest("#myPageProfileForm");
            if (profileForm) {
                event.preventDefault();
                submitProfileForm(profileForm);
                return;
            }

            const verifyForm = event.target.closest("#myPageSecurityVerifyForm");
            if (verifyForm) {
                event.preventDefault();
                submitSecurityVerifyForm(verifyForm);
                return;
            }

            const securityForm = event.target.closest("#myPageSecurityProfileForm");
            if (securityForm) {
                event.preventDefault();
                submitSecurityProfileForm(securityForm);
                return;
            }

            const passwordForm = event.target.closest("#myPagePasswordForm");
            if (passwordForm) {
                event.preventDefault();
                submitPasswordForm(passwordForm);
                return;
            }

            const withdrawForm = event.target.closest("#myPageWithdrawForm");
            if (withdrawForm) {
                event.preventDefault();
                submitWithdrawForm(withdrawForm);
            }
        });

        document.addEventListener("keydown", function (event) {
            if (event.key === "Escape") {
                closeMyPageModal();
            }
        });
    }

    async function openMyPageModal() {
        try {
            const response = await fetch(API.me, {
                credentials: "same-origin",
                cache: "no-store",
                headers: {
                    "Accept": "application/json",
                    "X-Requested-With": "XMLHttpRequest"
                }
            });

            const data = await readJson(response);
            if (!data || !data.loggedIn || !data.profile) {
                location.href = "/member/login";
                return;
            }

            state.currentUser = data.profile;
            state.myPage = data;
            profileEditMode = false;
            securityEditMode = false;
            securityUnlocked = Boolean(data.profile.socialLoginUser);
            passwordChangeMode = false;
            safeUpdateAuthWidget();
            renderMyPageModal(data, "profile");
        } catch (error) {
            alert("마이페이지 정보를 불러오지 못했습니다. 다시 시도해 주세요.");
        }
    }

    function renderCurrentMyPage(activeTab) {
        if (!state.myPage) {
            return;
        }
        renderMyPageModal(state.myPage, activeTab || getActiveTabName() || "profile");
    }

    function renderMyPageModal(data, activeTab) {
        removeExistingModal();

        const modal = document.createElement("div");
        modal.id = MODAL_ID;
        modal.className = "mypage-overlay";
        modal.innerHTML = renderModalContent(data, activeTab || "profile");
        document.body.appendChild(modal);
        document.body.classList.add("mypage-open");
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
    }

    function activateMyPageTab(tabName) {
        const modal = document.getElementById(MODAL_ID);
        if (!modal) {
            return;
        }

        modal.querySelectorAll("[data-mypage-tab]").forEach((button) => {
            const active = button.dataset.mypageTab === tabName;
            button.classList.toggle("active", active);
            button.setAttribute("aria-selected", String(active));
        });

        modal.querySelectorAll("[data-mypage-panel]").forEach((panel) => {
            panel.classList.toggle("active", panel.dataset.mypagePanel === tabName);
        });
    }

    function getActiveTabName() {
        const active = document.querySelector("[data-mypage-tab].active");
        return active ? active.dataset.mypageTab : null;
    }

    async function submitProfileForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateBasicProfilePayload(payload);
        if (clientMessage) {
            showMyPageMessage({ success: false, message: clientMessage });
            return;
        }

        const result = await postJson(API.profile, payload);
        showMyPageMessage(result);

        if (result.success && result.myPage) {
            state.currentUser = result.myPage.profile;
            state.myPage = result.myPage;
            profileEditMode = false;
            safeUpdateAuthWidget();
            renderMyPageModal(result.myPage, "profile");
            showMyPageMessage(result);
        }
    }

    async function submitSecurityVerifyForm(form) {
        const payload = formToObject(form);
        if (!payload.currentPassword) {
            showMyPageMessage({ success: false, message: "현재 비밀번호를 입력해 주세요." });
            return;
        }

        const result = await postJson(API.verifyPassword, payload);
        showMyPageMessage(result);

        if (result.success && result.myPage) {
            state.currentUser = result.myPage.profile;
            state.myPage = result.myPage;
            securityUnlocked = true;
            securityEditMode = false;
            passwordChangeMode = false;
            renderMyPageModal(result.myPage, "security");
            showMyPageMessage(result);
        }
    }

    async function submitSecurityProfileForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateSecurityProfilePayload(payload);
        if (clientMessage) {
            showMyPageMessage({ success: false, message: clientMessage });
            return;
        }

        const result = await postJson(API.security, payload);
        showMyPageMessage(result);

        if (result.success && result.myPage) {
            state.currentUser = result.myPage.profile;
            state.myPage = result.myPage;
            securityEditMode = false;
            safeUpdateAuthWidget();
            renderMyPageModal(result.myPage, "security");
            showMyPageMessage(result);
        }
    }

    async function submitPasswordForm(form) {
        const payload = formToObject(form);
        const clientMessage = validatePasswordPayload(payload);
        if (clientMessage) {
            showMyPageMessage({ success: false, message: clientMessage });
            return;
        }

        const result = await postJson(API.password, payload);
        showMyPageMessage(result);

        if (result.success) {
            passwordChangeMode = false;
            if (state.myPage) {
                renderMyPageModal(state.myPage, "password");
                showMyPageMessage(result);
            } else {
                form.reset();
            }
        }
    }

    async function submitWithdrawForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateWithdrawPayload(payload);
        if (clientMessage) {
            showMyPageMessage({ success: false, message: clientMessage });
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

            if (response.status === 401) {
                return {
                    success: false,
                    message: "로그인이 필요합니다. 다시 로그인해 주세요."
                };
            }

            const data = await readJson(response);
            return data || {
                success: false,
                message: "응답 형식이 올바르지 않습니다."
            };
        } catch (error) {
            return {
                success: false,
                message: "요청 처리 중 오류가 발생했습니다."
            };
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

    function showMyPageMessage(result) {
        const messageBox = document.querySelector("[data-mypage-message]");
        if (!messageBox) {
            return;
        }

        messageBox.textContent = result.message || "";
        messageBox.className = "mypage-message " + (result.success ? "success" : "error");
        messageBox.hidden = false;
        messageBox.scrollIntoView({ block: "nearest", behavior: "smooth" });
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

    function renderModalContent(data, activeTab) {
        const profile = data.profile;
        const activity = data.activity || {};
        const recentBoards = data.recentBoards || [];
        const recentChats = data.recentChats || [];
        const isAdmin = String(profile.role || "").toUpperCase() === "ADMIN";
        const loginMethodLabel = getLoginMethodLabel(profile);

        return `
            <section class="mypage-modal" role="dialog" aria-modal="true" aria-labelledby="myPageTitle">
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

                <nav class="mypage-tabs" aria-label="마이페이지 메뉴">
                    ${renderTab("profile", "👤", "내 정보", activeTab)}
                    ${renderTab("security", "🔒", "보안 설정", activeTab)}
                    ${renderTab("password", "🔑", "비밀번호 변경", activeTab)}
                    ${renderTab("social", "🔗", "소셜 연결", activeTab)}
                    ${renderTab("activity", "💬", "내 활동", activeTab)}
                    ${renderTab("withdraw", "⚠️", "회원 탈퇴", activeTab)}
                </nav>

                <div class="mypage-message" data-mypage-message hidden></div>

                <div class="mypage-panels">
                    <section class="mypage-panel ${activeTab === "profile" ? "active" : ""}" data-mypage-panel="profile">
                        ${renderProfilePanel(profile)}
                    </section>

                    <section class="mypage-panel ${activeTab === "security" ? "active" : ""}" data-mypage-panel="security">
                        ${renderSecurityPanel(profile)}
                    </section>

                    <section class="mypage-panel ${activeTab === "password" ? "active" : ""}" data-mypage-panel="password">
                        ${renderPasswordPanel(profile)}
                    </section>

                    <section class="mypage-panel ${activeTab === "social" ? "active" : ""}" data-mypage-panel="social">
                        ${renderSocialPanel(profile)}
                    </section>

                    <section class="mypage-panel ${activeTab === "activity" ? "active" : ""}" data-mypage-panel="activity">
                        ${renderActivityPanel(activity, recentBoards, recentChats)}
                    </section>

                    <section class="mypage-panel ${activeTab === "withdraw" ? "active" : ""}" data-mypage-panel="withdraw">
                        ${renderWithdrawPanel(isAdmin, profile)}
                    </section>
                </div>
            </section>
        `;
    }

    function renderTab(tabName, icon, label, activeTab) {
        return `
            <button type="button"
                    class="mypage-tab ${activeTab === tabName ? "active" : ""}"
                    data-mypage-tab="${tabName}"
                    aria-selected="${activeTab === tabName}">
                <span class="mypage-tab-icon">${icon}</span>
                <span>${label}</span>
            </button>
        `;
    }

    function renderProfilePanel(profile) {
        if (!profileEditMode) {
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
            securityUnlocked = true;
        }

        if (!securityUnlocked) {
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

        return `
            ${renderSecurityProfileForm(profile)}
        `;
    }

    function renderSecurityProfileForm(profile) {
        if (!securityEditMode) {
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
                    <span class="mypage-social-provider-icon ${provider}">${label.substring(0, 1)}</span>
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

        if (!passwordChangeMode) {
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

    async function readJson(response) {
        const contentType = response.headers.get("content-type") || "";
        if (!contentType.includes("application/json")) {
            return null;
        }

        return await response.json();
    }

    function startMyPageWhenReady() {
        if (typeof state === "undefined" || typeof renderAuthWidget !== "function") {
            setTimeout(startMyPageWhenReady, 30);
            return;
        }

        window.initializeMyPage();
    }

    startMyPageWhenReady();

})();
