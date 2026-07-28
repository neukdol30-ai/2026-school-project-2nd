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
    const loginMethodLabel = getAuthLoginMethodLabel();

    return `
    <article class="widget side-widget auth-widget">
        <div class="auth-content auth-logged-in">
            <div class="auth-profile-row">
                <button
                    type="button"
                    class="auth-profile-button"
                    data-mypage-open
                    title="마이페이지 열기"
                >
                    <span class="auth-profile-avatar" aria-hidden="true">
                        ${escapeHtml(displayName.slice(0, 1))}
                    </span>

                    <span class="auth-profile-info">
                        <strong class="auth-user-name">
                            ${escapeHtml(displayName)}님
                        </strong>

                        <span class="auth-role-badge">
                            ${escapeHtml(loginMethodLabel)}
                        </span>
                    </span>
                </button>

                <form
                    class="auth-logout-form"
                    action="/member/logout"
                    method="post"
                >
                    <button class="auth-logout-button" type="submit">
                        로그아웃
                    </button>
                </form>
            </div>

            <p class="auth-profile-desc">
                아이디를 누르면 마이페이지가 열립니다.
            </p>
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

function getAuthLoginMethodLabel() {
    if (!state.currentUser) {
        return "";
    }

    if (!state.currentUser.socialLoginUser) {
        return "일반 로그인";
    }

    const providers = String(state.currentUser.socialProviders || "")
        .split(",")
        .map((item) => item.trim().toUpperCase())
        .filter(Boolean)
        .map((provider) => {
            if (provider === "KAKAO") {
                return "카카오";
            }
            if (provider === "NAVER") {
                return "네이버";
            }
            return provider;
        });

    if (providers.length === 0) {
        return "소셜 로그인";
    }

    return providers.join(", ") + " 계정";
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
