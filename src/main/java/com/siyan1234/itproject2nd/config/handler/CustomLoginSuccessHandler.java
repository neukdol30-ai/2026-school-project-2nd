package com.siyan1234.itproject2nd.config.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 일반 로그인과 관리자 로그인의 성공 후 이동 경로를 분리하는 Handler입니다.
 */
@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        String loginType = request.getParameter("loginType");
        boolean adminLoginRequest = "admin".equals(loginType);
        boolean adminAuthority = hasAdminAuthority(authentication);

        if (adminLoginRequest) {
            if (adminAuthority) {
                response.sendRedirect(contextPath + "/admin");
                return;
            }

            SecurityContextHolder.clearContext();

            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }

            response.sendRedirect(contextPath + "/admin/login?error=role");
            return;
        }

        response.sendRedirect(contextPath + "/");
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
