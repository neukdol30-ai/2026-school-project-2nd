package com.siyan1234.itproject2nd.mypage.support;

import jakarta.servlet.http.HttpSession;

/**
 * 마이페이지 보안 설정 탭의 본인 확인 세션을 관리하는 유틸리티입니다.
 *
 * <p>일반 로그인 회원은 이메일/전화번호/주소처럼 개인정보를 수정하기 전에
 * 현재 비밀번호를 한 번 더 확인합니다. 이 인증 상태를 무기한 유지하지 않고
 * 일정 시간이 지나면 다시 인증하도록 처리합니다.</p>
 */
public final class MyPageSecurityVerification {

    private MyPageSecurityVerification() {
    }

    /** 본인 확인 성공 시 회원번호와 인증 시각을 세션에 저장합니다. */
    public static void markVerified(HttpSession session, Integer memberNo) {
        if (session == null || memberNo == null) {
            return;
        }

        session.setAttribute(MyPageSessionKeys.SECURITY_VERIFIED_MEMBER_NO, memberNo);
        session.setAttribute(MyPageSessionKeys.SECURITY_VERIFIED_AT, System.currentTimeMillis());
    }

    /** 현재 세션의 본인 확인 상태가 같은 회원이고, 유효 시간 안에 있는지 확인합니다. */
    public static boolean isVerified(HttpSession session, Integer memberNo) {
        return verificationFailureMessage(session, memberNo) == null;
    }

    /**
     * 본인 확인 상태가 유효하지 않을 때 사용자에게 보여줄 메시지를 반환합니다.
     * 유효하면 null을 반환합니다.
     */
    public static String verificationFailureMessage(HttpSession session, Integer memberNo) {
        if (session == null || memberNo == null) {
            return MyPageMessages.SECURITY_PASSWORD_VERIFY_REQUIRED;
        }

        Object verifiedMemberNo = session.getAttribute(MyPageSessionKeys.SECURITY_VERIFIED_MEMBER_NO);
        Object verifiedAt = session.getAttribute(MyPageSessionKeys.SECURITY_VERIFIED_AT);

        if (!memberNo.equals(verifiedMemberNo) || !(verifiedAt instanceof Long verifiedAtMillis)) {
            clear(session);
            return MyPageMessages.SECURITY_PASSWORD_VERIFY_REQUIRED;
        }

        if (System.currentTimeMillis() - verifiedAtMillis > MyPagePolicy.SECURITY_VERIFICATION_TIMEOUT_MILLIS) {
            clear(session);
            return MyPageMessages.SECURITY_PASSWORD_VERIFY_EXPIRED;
        }

        return null;
    }

    /** 본인 확인 세션 정보를 제거합니다. */
    public static void clear(HttpSession session) {
        if (session == null) {
            return;
        }

        session.removeAttribute(MyPageSessionKeys.SECURITY_VERIFIED_MEMBER_NO);
        session.removeAttribute(MyPageSessionKeys.SECURITY_VERIFIED_AT);
    }
}
