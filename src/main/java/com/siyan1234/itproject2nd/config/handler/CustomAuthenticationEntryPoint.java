package com.siyan1234.itproject2nd.config.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 인증되지 않은 사용자가 보호된 URL에 접근했을 때 이동할 로그인 화면을 결정합니다. */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();

        if (isAdminArea(contextPath, requestUri)) {
            response.sendRedirect(contextPath + "/admin/login");
            return;
        }

        response.sendRedirect(contextPath + "/member/login");
    }

    private boolean isAdminArea(String contextPath, String requestUri) {
        return requestUri.startsWith(contextPath + "/admin")
                || requestUri.startsWith(contextPath + "/chat/admin");
    }
}
