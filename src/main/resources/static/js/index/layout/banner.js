// 상단 글로벌 배너
function renderGlobalBanner() {
    const userMenu = state.currentUser
        ? `

            

            <button
                type="button"
                data-mypage-open
            >
                마이페이지
            </button>
            <button
                type="button"
                data-action="logout"
            >
                로그아웃
            </button>
        `
        : `
            <a href="/login">로그인</a>
        `;

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
            <a class="global-home-link" href="/" aria-label="홈으로 이동">
                <span class="global-logo-text" aria-hidden="true">
                    <span class="global-logo-letter">더</span>
                </span>
            </a>

            <div class="global-right-menu">
                <a class="global-service-link global-map-link" href="/map/kakao">
                    지도
                </a>

                <span class="global-calendar-label">
                    캘린더
                </span>
                
                <div class="global-user-menu">
                    ${userMenu}
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

// 로그인 및 설정 상태 변경 시 상단 메뉴 갱신
function updateGlobalBanner() {
    const globalBanner = document.querySelector(
        "[data-global-banner]"
    );

    if (!globalBanner) {
        return;
    }

    globalBanner.innerHTML = renderGlobalBanner();
}
