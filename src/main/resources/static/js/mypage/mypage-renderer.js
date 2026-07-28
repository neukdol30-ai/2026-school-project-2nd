/**
 * 마이페이지 모달의 HTML 생성만 담당합니다.
 * 이벤트 처리와 서버 통신을 분리해 화면 변경 시 영향 범위를 줄입니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMyPage = window.SecondProMyPage || {};
    const { TAB, TAB_ORDER, MODAL_ID } = namespace.config || {};
    const {
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
    } = namespace.utils || {};

    function createMyPageRenderer(viewState) {
        function normalizeTabName(tabName) {
            return TAB_ORDER.includes(tabName) ? tabName : TAB.profile;
        }

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

        return Object.freeze({ renderModalContent });
    }

    namespace.renderer = Object.freeze({ createMyPageRenderer });
})();
