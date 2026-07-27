package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminConsoleQuery;
import com.siyan1234.itproject2nd.admin.support.AdminConsoleModelAssembler;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 관리자 단일 콘솔 화면 Controller입니다.
 *
 * <p>HTTP 요청 파라미터를 {@link AdminConsoleQuery}로 묶고, 화면 조회와 Model 조립은
 * {@link AdminConsoleModelAssembler}에 위임해 Controller가 웹 계층 역할에 집중하도록 구성합니다.</p>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final AdminConsoleModelAssembler adminConsoleModelAssembler;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(
            @ModelAttribute AdminConsoleQuery query,
            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        adminConsoleModelAssembler.populate(model, query, loginAdmin);
        return "admin/dashboard";
    }
}
