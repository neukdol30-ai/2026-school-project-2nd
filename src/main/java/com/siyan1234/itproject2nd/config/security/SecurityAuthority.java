package com.siyan1234.itproject2nd.config.security;

/**
 * 권한 문자열을 한 곳에서 관리합니다.
 *
 * Spring Security의 hasRole("ADMIN")은 내부적으로 ROLE_ADMIN을 확인합니다.
 */
public final class SecurityAuthority {

    public static final String ADMIN = "ADMIN";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private SecurityAuthority() {
    }

    // 우리 서비스 약관에 아직 동의하지 않은 소셜 가입 대기 권한
    public static final String PENDING_SOCIAL = "PENDING_SOCIAL";

    public static final String ROLE_PENDING_SOCIAL = "ROLE_PENDING_SOCIAL";
}
