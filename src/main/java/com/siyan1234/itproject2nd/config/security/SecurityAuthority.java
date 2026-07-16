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
}
