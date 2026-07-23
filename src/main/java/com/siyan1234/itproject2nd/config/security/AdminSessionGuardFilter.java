package com.siyan1234.itproject2nd.config.security;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 관리자 권한을 DB 기준으로 매 요청마다 재확인하는 보안 필터입니다.
 *
 * Spring Security의 세션 권한은 로그인 시점의 권한을 들고 있기 때문에,
 * 로그인 이후 ADMIN -> USER로 강등되면 세션 안에는 예전 ROLE_ADMIN이 남아 있을 수 있습니다.
 * 이 필터는 /admin/** 요청마다 member 테이블을 다시 조회해서 현재도 활성 ADMIN인지 확인합니다.
 */
@Component
@RequiredArgsConstructor
public class AdminSessionGuardFilter extends OncePerRequestFilter {

    private static final String ADMIN_PATH = "/admin";
    private static final String ADMIN_LOGIN_PATH = "/admin/login";
    private static final String ADMIN_AUTH_STATUS_PATH = "/admin/auth/status";
    private static final String ADMIN_ROLE_ERROR_PATH = "/admin/login?error=role";

    private final MemberService memberService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isAdminProtectedRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Integer loginMemberNo = resolveLoginMemberNo();
        MemberDto latestMember = loginMemberNo == null ? null : memberService.findByNo(loginMemberNo);

        if (isActiveAdmin(latestMember)) {
            filterChain.doFilter(request, response);
            return;
        }

        invalidateLoginSession(request);
        handleInvalidAdmin(request, response);
    }

    private boolean isAdminProtectedRequest(HttpServletRequest request) {
        String path = request.getServletPath();

        if (path == null || !path.startsWith(ADMIN_PATH)) {
            return false;
        }

        return !ADMIN_LOGIN_PATH.equals(path);
    }

    private Integer resolveLoginMemberNo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            MemberDto memberDto = customUserDetails.getMemberDto();
            return memberDto == null ? null : memberDto.getNo();
        }

        if (principal instanceof MemberDto memberDto) {
            return memberDto.getNo();
        }

        return null;
    }

    private boolean isActiveAdmin(MemberDto memberDto) {
        return memberDto != null
                && SecurityAuthority.ADMIN.equalsIgnoreCase(memberDto.getRole())
                && !memberDto.isBanned();
    }

    private void invalidateLoginSession(HttpServletRequest request) {
        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void handleInvalidAdmin(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (isAdminAuthStatusRequest(request) || isAjaxRequest(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"activeAdmin\":false,\"redirectUrl\":\""
                    + request.getContextPath()
                    + ADMIN_ROLE_ERROR_PATH
                    + "\"}");
            return;
        }

        response.sendRedirect(request.getContextPath() + ADMIN_ROLE_ERROR_PATH);
    }

    private boolean isAdminAuthStatusRequest(HttpServletRequest request) {
        return ADMIN_AUTH_STATUS_PATH.equals(request.getServletPath());
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"));
    }
}