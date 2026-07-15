package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.CustomUserDetailService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 로그인 실패 시 일반 로그인과 관리자 로그인 화면으로 각각 돌려보내는 Handler입니다. */
@Component
@RequiredArgsConstructor
public class CustomLoginFailureHandler implements AuthenticationFailureHandler {

    public static final String LOGIN_ERROR_MESSAGE_SESSION_KEY = "loginErrorMessage";

    private static final String ADMIN_LOGIN_TYPE = "admin";
    private static final String MEMBER_ID_PARAMETER = "memberId";
    private static final String BANNED_ERROR_CODE = "banned";
    private static final String COMMON_ERROR_CODE = "true";

    private final MemberDao memberDao;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String errorCode = COMMON_ERROR_CODE;

        MemberDto loginAttemptMember = findLoginAttemptMember(request);
        if (isBannedLoginFailure(exception, loginAttemptMember)) {
            errorCode = BANNED_ERROR_CODE;
            saveLoginErrorMessage(request, resolveBanReason(exception, loginAttemptMember));
        }

        String redirectPath = isAdminLoginRequest(request)
                ? SecurityPaths.ADMIN_LOGIN + "?error=" + errorCode
                : SecurityPaths.MEMBER_LOGIN + "?error=" + errorCode;

        response.sendRedirect(SecurityPaths.withContextPath(request, redirectPath));
    }

    private boolean isAdminLoginRequest(HttpServletRequest request) {
        return ADMIN_LOGIN_TYPE.equals(request.getParameter("loginType"));
    }

    private MemberDto findLoginAttemptMember(HttpServletRequest request) {
        String memberId = request.getParameter(MEMBER_ID_PARAMETER);
        if (memberId == null || memberId.isBlank()) {
            return null;
        }

        try {
            return memberDao.findByMemberId(memberId.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isBannedLoginFailure(AuthenticationException exception, MemberDto loginAttemptMember) {
        return CustomUserDetailService.isBannedLoginException(exception)
                || (loginAttemptMember != null && loginAttemptMember.isBanned());
    }

    private String resolveBanReason(AuthenticationException exception, MemberDto loginAttemptMember) {
        if (CustomUserDetailService.isBannedLoginException(exception)) {
            return CustomUserDetailService.extractBanReason(exception);
        }

        if (loginAttemptMember != null && loginAttemptMember.isBanned()) {
            return loginAttemptMember.displayBanReason();
        }

        return "관리자에 의해 이용이 제한된 계정입니다.";
    }

    private void saveLoginErrorMessage(HttpServletRequest request, String message) {
        HttpSession session = request.getSession();
        session.setAttribute(LOGIN_ERROR_MESSAGE_SESSION_KEY, message);
    }
}
