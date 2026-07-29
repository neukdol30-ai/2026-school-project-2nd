package com.siyan1234.itproject2nd.config.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Security와 Interceptor에서 공통으로 사용하는 URL 경로 모음입니다.
 * <p>
 * 목적:
 * - SecurityConfig의 requestMatchers를 짧게 유지
 * - 인증/권한 예외 Handler의 redirect 경로 중복 제거
 * - 방문 기록 제외 경로를 WebMvcConfig와 Interceptor에서 동일하게 사용
 */
public final class SecurityPaths {

    public static final String HOME = "/";
    public static final String MEMBER_LOGIN = "/member/login";

    // 일반 회원가입과 소셜 약관 동의 화면에서 이용약관 전문을 열 때 사용하는 주소
    public static final String MEMBER_TERMS = "/member/terms";

    // 일반 회원가입과 소셜 약관 동의 화면에서 개인정보 전문을 열 때 사용하는 주소
    public static final String MEMBER_PRIVACY = "/member/privacy";

    // 신규 소셜 회원을 서비스 약관 동의 화면으로 보낼 때 사용하는 주소
    public static final String MEMBER_TERMS_AGREE = "/member/terms-agree";

    // 신규 소셜 사용자가 약관에 동의하지 않고 가입을 취소할 때 쓰는 POST 요청 주소
    public static final String MEMBER_TERMS_AGREE_CANCEL = "/member/terms-agree/cancel";

    public static final String ADMIN_LOGIN = "/admin/login";
    public static final String ADMIN_HOME = "/admin";
    public static final String ADMIN_CHATS = "/admin?view=chats";

    public static final String[] PUBLIC_MATCHERS = {
            "/", // 메인
            "/index.html",
            "/member/login", // 일반 사용자 로그인 화면
            "/member/signup", // 회원가입 화면
            "/member/signup/**", // 회원가입 이메일 인증번호 발송, 확인 주소 접근 허용
            MEMBER_TERMS, // 로그인 전에도 이용약관 전문을 확인할 수 있도록 허용
            MEMBER_PRIVACY, // 로그인 전에도 개인정보 전문을 확인할 수 있도록 허용
            "/member/exists", // 아이디 중복 확인
            "/member/exists-nickname", // 닉네임 중복 확인(회원가입 중 = 로그인 전에도 호출) 없으면 403
            // 비로그인 사용자 요청 -> Spring Security 필터 검사 -> PUBLIC_MATCHERS에 포함된 주소면 통과 -> 이후 MemberController가 요청 처리
            "/member/find-id", // 아이디 찾기 화면에 비로그인 사용자 접근 허용
            "/member/find-id/**", // 아이디 찾기 인증번호 발송·확인·결과 주소 접근 허용
            "/member/find-password", // 비밀번호 찾기 화면에 비로그인 사용자 접근 허용
            "/member/find-password/**", // 비밀번호 찾기 인증번호 발송·확인 주소 접근 허용
            "/member/reset-password", // 새 비밀번호 입력 화면과 변경 요청 접근 허용
            "/admin/login", // 관리자 로그인 화면
            "/mypage/me", // 메인 페이지 로그인 상태 확인용 공개 조회 API
            "/map/kakao", // 카카오맵 기능 테스트 화면
            "/kakao/authorize", // 카카오 동의
            "/kakao/callback", // 카카오 인가 코드 토큰발급
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico", // 브라우저가 탭 아이콘용으로 자동 요청하는 주소. 우리가 링크한 적 없어도 매 페이지마다 요청됨. 없으면 비로그인 상태에서 302로 로그인 화면에 튕겨 요청 2건이 낭비되고 탭 아이콘도 안 뜸
            "/api/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/error", // 필수, 예외 발생 시 Spring Boot가 /error로 내부 포워딩. Security 6은 그 포워딩도 인가 재검사. 없으면 비로그인 상태 예외 -> 에러 화면 대신 로그인으로 302 (에러 은폐)
            // 게시판 공개 주소
            "/board/list",       // 전체 게시글 목록
            "/board/notice",     // 공지사항 목록
            "/board/question",   // 문의 게시판 목록
            "/board/search",     // 게시글 검색
            "/board/detail/**",  // 게시글 상세
            "/board/write",      // 문의글 작성 화면 및 작성 처리
            "/board/image/upload",
            "/board/guest/update/**",
            "/board/guest/delete/**"
    };

    public static final String[] VISIT_LOG_EXCLUDE_PATTERNS = {
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/api/**",
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

        if (uri == null || !"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        /*
         * 브라우저 화면 이동(document/iframe)만 방문으로 기록합니다.
         * fetch/XHR 요청까지 기록하면 최근 경로가 /api/...로 덮어써질 수 있습니다.
         * Sec-Fetch-Dest가 없는 클라이언트는 URI 규칙으로 한 번 더 판정합니다.
         */
        String fetchDestination = request.getHeader("Sec-Fetch-Dest");
        if (fetchDestination != null
                && !fetchDestination.isBlank()
                && !"document".equalsIgnoreCase(fetchDestination)
                && !"iframe".equalsIgnoreCase(fetchDestination)) {
            return false;
        }

        return !uri.startsWith("/css/")
                && !uri.startsWith("/js/")
                && !uri.startsWith("/images/")
                && !uri.startsWith("/favicon")
                && !uri.startsWith("/api/")
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
