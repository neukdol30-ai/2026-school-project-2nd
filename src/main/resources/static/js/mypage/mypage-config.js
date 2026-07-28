/**
 * 마이페이지 API 주소, 탭 이름, 공통 메시지를 한 곳에서 관리합니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMyPage = window.SecondProMyPage || {};
    const TAB = Object.freeze({
        profile: "profile",
        security: "security",
        password: "password",
        social: "social",
        activity: "activity",
        withdraw: "withdraw"
    });

    namespace.config = Object.freeze({
        API: Object.freeze({
            me: "/mypage/me",
            profile: "/mypage/profile",
            verifyPassword: "/mypage/verify-password",
            security: "/mypage/security",
            password: "/mypage/password",
            withdraw: "/mypage/withdraw"
        }),
        TAB,
        TAB_ORDER: Object.freeze([
            TAB.profile,
            TAB.security,
            TAB.password,
            TAB.social,
            TAB.activity,
            TAB.withdraw
        ]),
        MODAL_ID: "myPageModal",
        LOGIN_URL: "/member/login",
        SESSION_REDIRECT_DELAY_MS: 900,
        PHONE_PATTERN: /^010-[0-9]{4}-[0-9]{4}$/,
        WITHDRAW_CONFIRM_TEXT: "회원탈퇴",
        SECURITY_VERIFY_REQUIRED_MESSAGE: "개인정보 수정을 위해 현재 비밀번호 인증이 필요합니다.",
        SECURITY_VERIFY_EXPIRED_MESSAGE: "본인 확인 시간이 만료되었습니다. 다시 현재 비밀번호를 인증해 주세요.",
        ERROR_MESSAGE: Object.freeze({
            loginExpired: "로그인이 만료되었습니다. 다시 로그인해 주세요.",
            forbidden: "접근 권한이 없습니다.",
            badRequest: "요청 정보가 올바르지 않습니다.",
            server: "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
            network: "네트워크 연결을 확인한 뒤 다시 시도해 주세요.",
            invalidResponse: "응답 형식이 올바르지 않습니다.",
            unknown: "처리 결과를 확인할 수 없습니다."
        })
    });
})();
