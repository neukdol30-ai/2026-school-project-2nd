package com.siyan1234.itproject2nd.config.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 로그인 실패 시 일반 로그인과 관리자 로그인 화면으로 각각 돌려보내는 Handler입니다. */
@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String contextPath = request.getContextPath();
        String loginType = request.getParameter("loginType");

        if ("admin".equals(loginType)) {
            response.sendRedirect(contextPath + "/admin/login?error=true");
            return;
        }

        response.sendRedirect(contextPath + "/member/login?error=true");
    }
}
