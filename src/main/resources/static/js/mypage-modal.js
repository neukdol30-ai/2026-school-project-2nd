(function () {
    const API = {
        me: "/mypage/me",
        profile: "/mypage/profile",
        password: "/mypage/password",
        withdraw: "/mypage/withdraw"
    };

    const MODAL_ID = "myPageModal";
    const PHONE_PATTERN = /^010-[0-9]{4}-[0-9]{4}$/;
    let initialized = false;

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

            const modal = document.getElementById(MODAL_ID);
            if (modal && event.target === modal) {
                closeMyPageModal();
            }
        });

        document.addEventListener("input", function (event) {
            const phoneInput = event.target.closest("[data-mypage-phone]");
            if (phoneInput) {
                phoneInput.value = formatPhoneValue(phoneInput.value);
                return;
            }
        });

        document.addEventListener("submit", function (event) {
            const profileForm = event.target.closest("#myPageProfileForm");
            if (profileForm) {
                event.preventDefault();
                submitProfileForm(profileForm);
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
            safeUpdateAuthWidget();
            renderMyPageModal(data, "profile");
        } catch (error) {
            alert("마이페이지 정보를 불러오지 못했습니다. 다시 시도해 주세요.");
        }
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
            button.classList.toggle("active", button.dataset.mypageTab === tabName);
        });

        modal.querySelectorAll("[data-mypage-panel]").forEach((panel) => {
            panel.classList.toggle("active", panel.dataset.mypagePanel === tabName);
        });
    }

    async function submitProfileForm(form) {
        const payload = formToObject(form);
        const clientMessage = validateProfilePayload(payload);
        if (clientMessage) {
            showMyPageMessage({ success: false, message: clientMessage });
            return;
        }

        const result = await postJson(API.profile, payload);
        showMyPageMessage(result);

        if (result.success && result.myPage) {
            state.currentUser = result.myPage.profile;
            state.myPage = result.myPage;
            safeUpdateAuthWidget();
            renderMyPageModal(result.myPage, "profile");
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
            form.reset();
        }
    }

    async function submitWithdrawForm(form) {
        const payload = formToObject(form);

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

    function validateProfilePayload(payload) {
        if (payload.phone && !PHONE_PATTERN.test(payload.phone)) {
            return "전화번호는 010-0000-0000 형식으로 입력해 주세요.";
        }

        if (payload.birthDate && payload.birthDate > todayDateInput()) {
            return "생년월일은 오늘 이후 날짜로 설정할 수 없습니다.";
        }

        return null;
    }

    function validatePasswordPayload(payload) {
        if (payload.currentPassword && payload.newPassword && payload.currentPassword === payload.newPassword) {
            return "현재 사용 중인 비밀번호와 같은 비밀번호로는 변경할 수 없습니다.";
        }

        return null;
    }

    function renderModalContent(data, activeTab) {
        const profile = data.profile;
        const activity = data.activity || {};
        const recentBoards = data.recentBoards || [];
        const recentChats = data.recentChats || [];
        const isAdmin = String(profile.role || "").toUpperCase() === "ADMIN";

        return `
            <section class="mypage-modal" role="dialog" aria-modal="true" aria-labelledby="myPageTitle">
                <button class="mypage-close" type="button" data-mypage-close aria-label="마이페이지 닫기">×</button>

                <header class="mypage-profile-header">
                    <div class="mypage-avatar-wrap">
                        ${profile.profileImage ? `
                            <img class="mypage-avatar-img" src="${html(profile.profileImage)}" alt="프로필 이미지">
                        ` : `
                            <div class="mypage-avatar-text">${html(profile.avatarText || "U")}</div>
                        `}
                    </div>
                    <div class="mypage-profile-main">
                        <h2 id="myPageTitle">${html(profile.displayName || profile.memberId)}님의 마이페이지</h2>
                        <p>${html(profile.memberId)} · ${html(profile.role || "USER")}</p>
                        <span>최근 로그인 ${formatDateTime(profile.lastLoginDate)}</span>
                    </div>
                </header>

                <nav class="mypage-tabs" aria-label="마이페이지 메뉴">
                    ${renderTab("profile", "👤", "내 정보", activeTab)}
                    ${renderTab("security", "🔒", "보안 설정", activeTab)}
                    ${renderTab("activity", "📊", "내 활동", activeTab)}
                    ${renderTab("withdraw", "⚠️", "회원 탈퇴", activeTab)}
                </nav>

                <div class="mypage-message" data-mypage-message hidden></div>

                <div class="mypage-panels">
                    <section class="mypage-panel ${activeTab === "profile" ? "active" : ""}" data-mypage-panel="profile">
                        ${renderProfileForm(profile)}
                    </section>

                    <section class="mypage-panel ${activeTab === "security" ? "active" : ""}" data-mypage-panel="security">
                        ${renderPasswordForm()}
                    </section>

                    <section class="mypage-panel ${activeTab === "activity" ? "active" : ""}" data-mypage-panel="activity">
                        ${renderActivityPanel(activity, recentBoards, recentChats)}
                    </section>

                    <section class="mypage-panel ${activeTab === "withdraw" ? "active" : ""}" data-mypage-panel="withdraw">
                        ${renderWithdrawPanel(isAdmin)}
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

    function renderProfileForm(profile) {
        return `
            <form id="myPageProfileForm" class="mypage-form">
                <div class="mypage-form-grid">
                    ${readonlyInput("아이디", profile.memberId)}
                    ${readonlyInput("권한", profile.role || "USER")}
                    ${input("이름", "name", profile.name)}
                    ${input("닉네임", "nickname", profile.nickname, true)}
                    ${input("이메일", "email", profile.email, false, "email")}
                    ${input("전화번호", "phone", profile.phone, false, "tel", "010-0000-0000", "data-mypage-phone inputmode=\"numeric\" maxlength=\"13\"")}
                    ${input("생년월일", "birthDate", toDateInput(profile.birthDate), false, "date", "", `max=\"${todayDateInput()}\"`)}
                    ${selectGender(profile.gender)}
                    ${input("우편번호", "postcode", profile.postcode)}
                    ${input("주소", "address", profile.address)}
                    ${input("상세주소", "detailAddress", profile.detailAddress)}
                    ${input("프로필 이미지 URL", "profileImage", profile.profileImage)}
                </div>
                <div class="mypage-form-footer">
                    <button type="button" class="mypage-secondary-btn" data-mypage-close>닫기</button>
                    <button type="submit" class="mypage-primary-btn">저장</button>
                </div>
            </form>
        `;
    }

    function renderPasswordForm() {
        return `
            <form id="myPagePasswordForm" class="mypage-form mypage-narrow-form">
                <p class="mypage-guide">비밀번호는 대문자, 소문자, 숫자를 포함한 8~20자로 입력해 주세요.</p>
                ${input("현재 비밀번호", "currentPassword", "", true, "password")}
                ${input("새 비밀번호", "newPassword", "", true, "password")}
                ${input("새 비밀번호 확인", "newPasswordCheck", "", true, "password")}
                <div class="mypage-form-footer">
                    <button type="submit" class="mypage-primary-btn">비밀번호 변경</button>
                </div>
            </form>
        `;
    }

    function renderActivityPanel(activity, recentBoards, recentChats) {
        return `
            <div class="mypage-activity-grid">
                ${activityCard("작성 게시글", activity.boardCount || 0)}
                ${activityCard("답변 대기 문의", activity.waitingQuestionCount || 0)}
                ${activityCard("상담 내역", activity.chatCount || 0)}
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
                        <h3>최근 상담</h3>
                        <a href="/chat/history?from=mypage">상담 내역</a>
                    </div>
                    ${renderRecentChats(recentChats)}
                </section>
            </div>
        `;
    }

    function renderWithdrawPanel(isAdmin) {
        if (isAdmin) {
            return `
                <div class="mypage-danger-box">
                    <h3>관리자 계정 보호</h3>
                    <p>관리자 계정은 마이페이지에서 탈퇴할 수 없습니다. 권한과 계정 삭제는 관리자 콘솔에서 관리해 주세요.</p>
                </div>
            `;
        }

        return `
            <form id="myPageWithdrawForm" class="mypage-form mypage-narrow-form">
                <div class="mypage-danger-box">
                    <h3>회원 탈퇴</h3>
                    <p>탈퇴 후 계정 정보와 일부 이용 기록이 삭제되거나 작성자 없음으로 처리될 수 있습니다.</p>
                </div>
                ${input("현재 비밀번호", "password", "", true, "password")}
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

    function readonlyInput(label, value) {
        return `
            <label class="mypage-field">
                <span>${label}</span>
                <input type="text" value="${html(value || "-")}" readonly>
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
