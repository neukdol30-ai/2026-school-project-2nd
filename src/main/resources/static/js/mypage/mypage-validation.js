/**
 * 브라우저에서 즉시 안내할 수 있는 마이페이지 입력값 1차 검증입니다.
 * 실제 권한·비밀번호 검증은 서버가 최종 책임집니다.
 */
(() => {
    "use strict";

    const namespace = window.SecondProMyPage = window.SecondProMyPage || {};
    const { PHONE_PATTERN, WITHDRAW_CONFIRM_TEXT } = namespace.config || {};
    const { todayDateInput } = namespace.utils || {};

    function validateBasicProfilePayload(payload) {
        if (!payload.name) {
            return "이름을 입력해 주세요.";
        }

        if (!payload.nickname) {
            return "닉네임을 입력해 주세요.";
        }

        if (payload.nickname.length < 2 || payload.nickname.length > 10) {
            return "닉네임은 2~10자로 입력해 주세요.";
        }

        return null;
    }

    function validateSecurityProfilePayload(payload) {
        if (payload.phone && !PHONE_PATTERN.test(payload.phone)) {
            return "전화번호는 010-0000-0000 형식으로 입력해 주세요.";
        }

        if (payload.birthDate && payload.birthDate > todayDateInput()) {
            return "생년월일은 오늘 이후 날짜로 설정할 수 없습니다.";
        }

        return null;
    }

    function validatePasswordPayload(payload) {
        if (!payload.currentPassword || !payload.newPassword || !payload.newPasswordCheck) {
            return "현재 비밀번호, 새 비밀번호, 새 비밀번호 확인을 모두 입력해 주세요.";
        }

        if (payload.newPassword !== payload.newPasswordCheck) {
            return "새 비밀번호 확인이 일치하지 않습니다.";
        }

        if (payload.currentPassword === payload.newPassword) {
            return "현재 사용 중인 비밀번호와 같은 비밀번호로는 변경할 수 없습니다.";
        }

        return null;
    }

    function validateWithdrawPayload(payload) {
        if (payload.confirmText !== WITHDRAW_CONFIRM_TEXT) {
            return "회원 탈퇴를 진행하려면 확인 문구에 '회원탈퇴'를 정확히 입력해 주세요.";
        }

        return null;
    }

    namespace.validation = Object.freeze({
        validateBasicProfilePayload,
        validateSecurityProfilePayload,
        validatePasswordPayload,
        validateWithdrawPayload
    });
})();
