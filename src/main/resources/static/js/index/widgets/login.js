//로그인 위젯
function renderAuthWidget() {
    if (!state.currentUser) {
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
                    <input
                        id="loginUsername"
                        class="auth-input"
                        type="text"
                        value="${state.loginForm.username}"
                        placeholder="아이디"
                    >

                    <input
                        id="loginPassword"
                        class="auth-input"
                        type="password"
                        value="${state.loginForm.password}"
                        placeholder="비밀번호"
                    >

                    <button class="auth-login-button" data-action="login">
                        로그인
                    </button>

                    <button class="auth-register-button">
                        회원가입
                    </button>
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