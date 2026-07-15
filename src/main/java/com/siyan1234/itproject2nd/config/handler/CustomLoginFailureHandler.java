package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.service.CustomUserDetailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 로그인 실패 시 일반 로그인과 관리자 로그인 화면으로 각각 돌려보내는 Handler입니다. */
@Component
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    public static final String LOGIN_ERROR_MESSAGE_SESSION_KEY = "loginErrorMessage";

    private static final String ADMIN_LOGIN_TYPE = "admin";
    private static final String BANNED_ERROR_CODE = "banned";
    private static final String COMMON_ERROR_CODE = "true";

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = COMMON_ERROR_CODE;

        if (CustomUserDetailService.isBannedLoginException(exception)) {
            errorCode = BANNED_ERROR_CODE;
            saveLoginErrorMessage(request, CustomUserDetailService.extractBanReason(exception));
        }

        String redirectPath = isAdminLoginRequest(request)
                ? SecurityPaths.ADMIN_LOGIN + "?error=" + errorCode
                : SecurityPaths.MEMBER_LOGIN + "?error=" + errorCode;

        response.sendRedirect(SecurityPaths.withContextPath(request, redirectPath));
    }

    private boolean isAdminLoginRequest(HttpServletRequest request) {
        return ADMIN_LOGIN_TYPE.equals(request.getParameter("loginType"));
    }

    private void saveLoginErrorMessage(HttpServletRequest request, String message) {
        HttpSession session = request.getSession();
        session.setAttribute(LOGIN_ERROR_MESSAGE_SESSION_KEY, message);
    }
}
