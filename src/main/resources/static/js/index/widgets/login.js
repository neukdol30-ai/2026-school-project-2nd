//로그인 위젯
function renderAuthWidget() {
    if (!state.currentUser) {
        return `
    <article class="widget side-widget auth-widget">
        <div class="auth-content auth-logged-out">
            <p class="auth-message">
                로그인 하셔서 여러 서비스를 즐기세요.
            </p>

            <a
                class="auth-login-button"
                href="/member/login"
            >
                로그인
            </a>

            <nav class="auth-links" aria-label="계정 메뉴">
                <a href="/member/find-id">아이디 찾기</a>
                <a href="/member/find-password">비밀번호 찾기</a>
                <a href="/member/signup">회원가입</a>
            </nav>
        </div>
    </article>
`;
    }

    const displayName = getAuthDisplayName();

    return `
        <article class="widget side-widget auth-widget">
            <div class="widget-header">
                <div>
                    <div class="widget-title">🔐 계정</div>
                    <div class="widget-desc">
                        아이디를 누르면 마이페이지가 열립니다.
                    </div>
                </div>
            </div>

            <div class="auth-content">
                <button type="button" class="auth-user-button" data-mypage-open>
                    ${escapeHtml(displayName)}님
                </button>
                <p class="auth-sub-text">
                    ${escapeHtml(state.currentUser.memberId || "")} 로그인 중
                </p>

                <form class="auth-logout-form" action="/member/logout" method="post">
                    <button class="auth-logout-button" type="submit">
                        로그아웃
                    </button>
                </form>
            </div>
        </article>
    `;
}

function getAuthDisplayName() {
    if (!state.currentUser) {
        return "회원";
    }

    return state.currentUser.displayName
        || state.currentUser.nickname
        || state.currentUser.name
        || state.currentUser.memberId
        || "회원";
}

//로그인 계산 함수
function login() {
    if (!state.loginForm.username) {
        alert("아이디를 입력하세요.");
        return;
    }

    state.currentUser = {
        username: state.loginForm.username,
        nickname: state.loginForm.username
    };

    state.loginForm.password = "";
    render();
}
