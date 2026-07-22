// 상단 글로벌 배너
function renderGlobalBanner() {
    const userMenu = state.currentUser
        ? `
            <a href="/mypage">내 정보</a>
            <button data-action="logout">로그아웃</button>
        `
        : `
            <a href="/login">로그인</a>
        `;

    return `
        <nav class="global-banner" aria-label="전역 메뉴">
            <a
                class="global-home-link"
                href="/"
            >
                홈
            </a>

            <div class="global-user-menu">
                ${userMenu}
            </div>
        </nav>
    `;
}

// 로그인 상태 변경 시 배너 메뉴만 갱신
function updateGlobalBanner() {
    const globalBanner = document.querySelector(
        "[data-global-banner]"
    );

    if (!globalBanner) {
        return;
    }

    globalBanner.innerHTML = renderGlobalBanner();
}