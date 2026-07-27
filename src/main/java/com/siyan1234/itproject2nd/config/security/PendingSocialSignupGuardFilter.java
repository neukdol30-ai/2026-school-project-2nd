package com.siyan1234.itproject2nd.config.security;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 약관 동의 전 소셜 가입 대기 사용자가 다른 인증 필요 서비스를 이용하지 못하도록 막는 Security 필터
@Component
public class PendingSocialSignupGuardFilter extends OncePerRequestFilter {

    // 한 HTTP 요청마다 한 번 실행되는 필터 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof CustomUserDetails loginUser)
                || !loginUser.isPendingSocialSignup()) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = normalizePath(request);

        if (isAllowedPendingPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 약관 동의 전 다른 서비스 주소로 접근하면 Controller 실행 없이 약관 동의 화면으로 돌려보냄
        response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_TERMS_AGREE));
    }

    // 가입 대기 상태에서도 접근 가능한 경로인지 확인
    private boolean isAllowedPendingPath(String requestPath) {

        if (requestPath == null) {
            return false;
        }

        if (SecurityPaths.MEMBER_TERMS_AGREE.equals(requestPath)) {
            return true;
        }

        if (SecurityPaths.MEMBER_TERMS_AGREE_CANCEL.equals(requestPath)) {
            return true;
        }

        if (SecurityPaths.MEMBER_TERMS.equals(requestPath) || SecurityPaths.MEMBER_PRIVACY.equals(requestPath)) {
            return true;
        }

        return requestPath.startsWith("/css/")
                || requestPath.startsWith("/js/")
                || requestPath.startsWith("/images/")
                || requestPath.startsWith("/favicon")
                || requestPath.startsWith("/error");
    }

    private String normalizePath(HttpServletRequest request) {

        if (request == null || request.getRequestURI() == null) {
            return null;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }
}