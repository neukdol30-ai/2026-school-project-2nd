// 상단 서비스 메뉴 사용자 영역
function renderGlobalServiceUserArea() {
    if (!state.currentUser) {
        return `
            <a
                class="global-service-login-link"
                href="/member/login"
            >
                <strong>로그인하세요</strong>
                <span aria-hidden="true">›</span>
            </a>

            <p class="global-service-login-desc">
                로그인하고 여러 서비스를 편리하게 이용하세요.
            </p>
        `;
    }

    const displayName =
        typeof getAuthDisplayName === "function"
            ? getAuthDisplayName()
            : String(
                state.currentUser.nickname
                || state.currentUser.name
                || state.currentUser.username
                || state.currentUser.memberId
                || "회원"
            );

    const avatarText =
        displayName.trim().charAt(0) || "회";

    return `
        <div class="global-service-profile">
            <button
                class="global-service-profile-button"
                type="button"
                data-mypage-open
            >
                <span
                    class="global-service-avatar"
                    aria-hidden="true"
                >
                    ${avatarText}
                </span>

                <span class="global-service-profile-text">
                    <strong>${displayName}님</strong>
                    <span>마이페이지로 이동</span>
                </span>

                <span
                    class="global-service-profile-arrow"
                    aria-hidden="true"
                >
                    ›
                </span>
            </button>

            <button
                class="global-service-logout-button"
                type="button"
                data-action="logout"
            >
                로그아웃
            </button>
        </div>
    `;
}

// 서비스 바로가기 메뉴
function renderGlobalServiceMenu() {
    return `
        <div
            class="global-service-menu"
            data-global-service-menu
        >
            <button
                class="global-service-menu-button"
                type="button"
                data-action="toggle-service-menu"
                aria-label="서비스 메뉴 열기"
                aria-expanded="false"
                aria-controls="globalServicePanel"
            >
                <span aria-hidden="true"></span>
                <span aria-hidden="true"></span>
                <span aria-hidden="true"></span>
            </button>

            <section
                id="globalServicePanel"
                class="global-service-panel"
                data-global-service-panel
                aria-label="서비스 바로가기"
                hidden
            >
                <div class="global-service-panel-user">
                    ${renderGlobalServiceUserArea()}
                </div>

                <div class="global-service-panel-divider"></div>

                <div class="global-service-shortcut-area">
                    <h2>바로가기</h2>

                    <div class="global-service-shortcuts">
                        <a
                            class="global-service-shortcut"
                            href="/calendar"
                            data-service-menu-link
                        >
                            <span
                                class="global-service-shortcut-icon"
                                aria-hidden="true"
                            >
                                📅
                            </span>
                            <span>캘린더</span>
                        </a>

                        <a
                            class="global-service-shortcut"
                            href="/map/kakao"
                            data-service-menu-link
                        >
                            <span
                                class="global-service-shortcut-icon"
                                aria-hidden="true"
                            >
                                📍
                            </span>
                            <span>지도</span>
                        </a>
                    </div>
                </div>
            </section>
        </div>
    `;
}

// 상단 글로벌 배너
function renderGlobalBanner() {
    const settingsMenu = state.isEditMode
        ? `
            <button
                type="button"
                data-action="finish-layout-edit"
            >
                배치 완료
            </button>
        `
        : `
            <button
                type="button"
                data-action="toggle-edit"
            >
                <span
                    class="global-settings-icon"
                    aria-hidden="true"
                >
                    ⚙
                </span>
                <span>환경설정</span>
            </button>
        `;

    return `
        <nav class="global-banner" aria-label="전역 메뉴">
            <div class="global-left-menu">
                ${renderGlobalServiceMenu()}

                <a class="global-home-link" href="/" aria-label="홈으로 이동">
                    <span class="global-logo-text" aria-hidden="true">
                        <span class="global-logo-letter">더</span>
                    </span>
                </a>
            </div>

            <div class="global-right-menu">
                <a class="global-service-link global-map-link" href="/map/kakao">
                    <span
                        class="global-map-icon"
                        aria-hidden="true"
                    >
                        📍
                    </span>
                    <span>지도</span>
                </a>

                <span
                    class="global-menu-divider"
                    aria-hidden="true"
                ></span>

                <div class="global-user-menu">
                    <button
                        type="button"
                        data-mypage-open
                    >
                        마이페이지
                    </button>
                </div>

                <span
                    class="global-menu-divider"
                    aria-hidden="true"
                ></span>

                <div class="global-settings-menu">
                    ${settingsMenu}
                </div>
            </div>
        </nav>
    `;
}

// 상단 메뉴 갱신
function updateGlobalBanner() {
    const globalBanner = document.querySelector(
        "[data-global-banner]"
    );

    if (!globalBanner) {
        return;
    }

    globalBanner.innerHTML = renderGlobalBanner();
}
