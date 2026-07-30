package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminConsoleQuery;
import com.siyan1234.itproject2nd.admin.support.AdminConsoleModelAssembler;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.admin.support.AdminView;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        boolean redirectRequired = adminConsoleModelAssembler.populate(model, query, loginAdmin);

        if (redirectRequired) {
            addCanonicalPageAttributes(redirectAttributes, query);
            return AdminRoutes.ADMIN_HOME;
        }

        return "admin/dashboard";
    }

    /** 주소창의 잘못된 화면·페이지 값을 검색 조건을 유지한 정규 URL로 보정합니다. */
    private void addCanonicalPageAttributes(
            RedirectAttributes redirectAttributes,
            AdminConsoleQuery query
    ) {
        AdminView activeView = AdminView.from(query.getView());
        redirectAttributes.addAttribute("view", activeView.getCode());

        switch (activeView) {
            case MEMBERS -> {
                redirectAttributes.addAttribute("memberKeyword", query.getMemberKeyword());
                redirectAttributes.addAttribute("memberPage", query.getMemberPage());
                redirectAttributes.addAttribute("memberSize", query.getMemberSize());
            }
            case CHATS -> {
                redirectAttributes.addAttribute("chatStatus", query.getChatStatus());
                redirectAttributes.addAttribute("chatCategory", query.getChatCategory());
                redirectAttributes.addAttribute("chatKeyword", query.getChatKeyword());
                redirectAttributes.addAttribute("chatPage", query.getChatPage());
                redirectAttributes.addAttribute("chatSize", query.getChatSize());
            }
            case BOARDS -> {
                redirectAttributes.addAttribute("boardCategory", query.getBoardCategory());
                redirectAttributes.addAttribute("boardKeyword", query.getBoardKeyword());
                redirectAttributes.addAttribute("boardPage", query.getBoardPage());
                redirectAttributes.addAttribute("boardSize", query.getBoardSize());
            }
            case VISITS -> {
                redirectAttributes.addAttribute("visitKeyword", query.getVisitKeyword());
                redirectAttributes.addAttribute("visitPage", query.getVisitPage());
                redirectAttributes.addAttribute("visitSize", query.getVisitSize());
            }
            default -> {
                // 목록 화면이 아닌 경우 별도 페이지 파라미터가 없습니다.
            }
        }
    }
}
