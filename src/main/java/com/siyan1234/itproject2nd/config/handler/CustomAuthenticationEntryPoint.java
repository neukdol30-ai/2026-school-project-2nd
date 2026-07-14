package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
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
        String redirectPath = SecurityPaths.isAdminArea(request)
                ? SecurityPaths.ADMIN_LOGIN
                : SecurityPaths.MEMBER_LOGIN;

        response.sendRedirect(SecurityPaths.withContextPath(request, redirectPath));
    }
}
