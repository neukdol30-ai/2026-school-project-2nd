package com.siyan1234.itproject2nd.config.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Security와 Interceptor에서 공통으로 사용하는 URL 경로 모음입니다.
 *
 * 목적:
 * - SecurityConfig의 requestMatchers를 짧게 유지
 * - 인증/권한 예외 Handler의 redirect 경로 중복 제거
 * - 방문 기록 제외 경로를 WebMvcConfig와 Interceptor에서 동일하게 사용
 */
public final class SecurityPaths {

    public static final String HOME = "/";
    public static final String MEMBER_LOGIN = "/member/login";
    public static final String ADMIN_LOGIN = "/admin/login";
    public static final String ADMIN_HOME = "/admin";
    public static final String ADMIN_CHATS = "/admin?view=chats";

    public static final String[] PUBLIC_MATCHERS = {
            "/", // 메인
            "/index.html",
            "/member/login", // 일반 사용자 로그인 화면
            "/member/signup", // 회원가입 화면
            "/member/exists", // 아이디 중복 확인
            "/member/exists-nickname", // 닉네임 중복 확인(회원가입 중 = 로그인 전에도 호출) 없으면 403
            "/admin/login", // 관리자 로그인 화면
            "/kakao/authorize", // 카카오 동의
            "/kakao/callback", // 카카오 인가 코드 토큰발급
            "/css/**",
            "/js/**",
            "/images/**",
            "/api/**",
            "/error" // 필수, 예외 발생 시 Spring Boot가 /error로 내부 포워딩. Security 6은 그 포워딩도 인가 재검사. 없으면 비로그인 상태 예외 -> 에러 화면 대신 로그인으로 302 (에러 은폐)
    };

    public static final String[] VISIT_LOG_EXCLUDE_PATTERNS = {
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/ws/**",
            "/error"
    };

    private SecurityPaths() {
    }

    public static String withContextPath(HttpServletRequest request, String path) {
        return request.getContextPath() + path;
    }

    public static boolean isAdminArea(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        return startsWithContextPath(requestUri, contextPath, "/admin")
                || startsWithContextPath(requestUri, contextPath, "/chat/admin");
    }

    public static boolean isVisitLogTarget(HttpServletRequest request) {
        String uri = normalizeUri(request);

        if (uri == null) {
            return false;
        }

        return !uri.startsWith("/css/")
                && !uri.startsWith("/js/")
                && !uri.startsWith("/images/")
                && !uri.startsWith("/favicon")
                && !uri.startsWith("/ws/")
                && !uri.startsWith("/error");
    }

    private static boolean startsWithContextPath(String requestUri, String contextPath, String targetPath) {
        if (requestUri == null) {
            return false;
        }

        String prefix = (contextPath == null ? "" : contextPath) + targetPath;
        return requestUri.startsWith(prefix);
    }

    private static String normalizeUri(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null) {
            return null;
        }

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }

        return uri;
    }
}
