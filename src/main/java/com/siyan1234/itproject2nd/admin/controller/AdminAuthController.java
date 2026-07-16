package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.config.handler.CustomLoginFailureHandler;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** 관리자 로그인 화면 Controller입니다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminAuthController {

    private final LoginMemberResolver loginMemberResolver;

    @GetMapping("/login")
    public String adminLogin(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = loginMemberResolver.fromPrincipal(customUserDetails);

        if (loginMemberResolver.isAdmin(loginUser)) {
            return AdminRoutes.ADMIN_HOME;
        }

        Object loginErrorMessage = session.getAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
        if (loginErrorMessage != null) {
            model.addAttribute("loginErrorMessage", loginErrorMessage);
            session.removeAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
        }

        model.addAttribute("loginUser", loginUser);
        return "admin/login";
    }
}
