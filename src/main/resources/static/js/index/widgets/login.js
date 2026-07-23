//로그인 위젯
function renderAuthWidget() {
    if (!state.currentUser) {
        return `
    <article class="widget side-widget auth-widget">
        <div class="widget-header">
            <div>
                <div class="widget-title">♙ 로그인</div>
                <div class="widget-desc">
                    로그인해서 여러 서비스를 이용해 보세요.
                </div>
            </div>
        </div>

        <div class="auth-content auth-logged-out">
            <p class="auth-message">
                로그인 후 나만의 대시보드를 이용할 수 있습니다.
            </p>

            <a
                class="auth-login-button"
                href="/login"
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

    return `
        <article class="widget side-widget auth-widget">
            <div class="widget-header">
                <div>
                    <div class="widget-title">🔐 계정</div>
                    <div class="widget-desc">
                        로그인하면 사용자 설정이 적용됩니다.
                    </div>
                </div>
            </div>

            <div class="auth-content">
                <p class="auth-user">
                    ${state.currentUser.nickname}님 로그인 중
                </p>

                <button class="auth-login-button" data-action="logout">
                    로그아웃
                </button>
            </div>
        </article>
    `;
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