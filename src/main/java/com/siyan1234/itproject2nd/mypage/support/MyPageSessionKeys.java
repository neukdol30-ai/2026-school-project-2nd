package com.siyan1234.itproject2nd.mypage.support;

public final class MyPageSessionKeys {

    /** 보안 설정 개인정보 수정 전 현재 비밀번호 인증을 완료한 회원번호를 저장하는 세션 키입니다. */
    public static final String SECURITY_VERIFIED_MEMBER_NO = "MYPAGE_SECURITY_VERIFIED_MEMBER_NO";

    /** 보안 설정 본인 확인을 완료한 시각을 밀리초 단위로 저장하는 세션 키입니다. */
    public static final String SECURITY_VERIFIED_AT = "MYPAGE_SECURITY_VERIFIED_AT";

    private MyPageSessionKeys() {
    }
}
