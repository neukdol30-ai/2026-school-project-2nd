package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityAuthority;
import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/** 일반 로그인과 관리자 로그인의 성공 후 이동 경로를 분리하는 Handler입니다. */
@Component
@RequiredArgsConstructor
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final String ADMIN_LOGIN_TYPE = "admin";

    private final MemberDao memberDao;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        updateLastLoginDate(authentication);

        if (!isAdminLoginRequest(request)) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.HOME));
            return;
        }

        if (hasAdminAuthority(authentication)) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.ADMIN_HOME));
            return;
        }

        clearAuthentication(request);
        response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.ADMIN_LOGIN + "?error=role"));
    }

    private void updateLastLoginDate(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            return;
        }

        MemberDto memberDto = customUserDetails.getMemberDto();

        if (memberDto == null || memberDto.getNo() == null) {
            return;
        }

        memberDao.updateLastLoginDate(memberDto.getNo());
        memberDto.setLastLoginDate(LocalDateTime.now());
    }

    private boolean isAdminLoginRequest(HttpServletRequest request) {
        return ADMIN_LOGIN_TYPE.equals(request.getParameter("loginType"));
    }

    private boolean hasAdminAuthority(Authentication authentication) {
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities()
                .stream()
                .anyMatch(authority -> SecurityAuthority.ROLE_ADMIN.equals(authority.getAuthority()));
    }

    private void clearAuthentication(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }
    }
}
