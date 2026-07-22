package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.config.handler.CustomLoginFailureHandler;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/** 관리자 로그인 화면 Controller입니다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminAuthController {

    private final LoginMemberResolver loginMemberResolver;
    private final AdminMemberService adminMemberService;

    @GetMapping("/login")
    public String adminLogin(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = loginMemberResolver.fromPrincipal(customUserDetails);

        if (loginUser != null && adminMemberService.isActiveAdmin(loginUser.getNo())) {
            return AdminRoutes.ADMIN_HOME;
        }

        boolean sessionInvalidated = false;
        if (loginUser != null && loginMemberResolver.isAdmin(loginUser)) {
            SecurityContextHolder.clearContext();
            session.invalidate();
            sessionInvalidated = true;
            loginUser = null;
        }

        if (!sessionInvalidated) {
            Object loginErrorMessage = session.getAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
            if (loginErrorMessage != null) {
                model.addAttribute("loginErrorMessage", loginErrorMessage);
                session.removeAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
            }
        }

        model.addAttribute("loginUser", loginUser);
        return "admin/login";
    }

    /**
     * 관리자 콘솔이 열려 있는 동안 현재 계정이 아직 활성 ADMIN인지 확인하는 엔드포인트입니다.
     * 권한이 변경되면 AdminSessionGuardFilter가 먼저 401 JSON을 반환합니다.
     */
    @GetMapping("/auth/status")
    @ResponseBody
    public Map<String, Object> adminAuthStatus(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        MemberDto loginUser = loginMemberResolver.fromPrincipal(customUserDetails);
        boolean activeAdmin = loginUser != null && adminMemberService.isActiveAdmin(loginUser.getNo());

        return Map.of(
                "activeAdmin", activeAdmin,
                "memberNo", loginUser == null ? 0 : loginUser.getNo()
        );
    }
}
