package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminConsoleQuery;
import com.siyan1234.itproject2nd.admin.support.AdminConsoleModelAssembler;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.admin.support.AdminView;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

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

    private static final Set<String> REMOVED_PAGE_SIZE_PARAMETERS = Set.of(
            "memberSize",
            "chatSize",
            "boardSize",
            "visitSize"
    );

    private final AdminConsoleModelAssembler adminConsoleModelAssembler;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(
            @ModelAttribute AdminConsoleQuery query,
            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        boolean redirectRequired = adminConsoleModelAssembler.populate(model, query, loginAdmin);

        /*
         * 페이지 크기는 서버에서 10건으로 고정합니다.
         * 과거 URL이나 주소창 조작으로 size 파라미터가 들어오면 이를 제거한 정규 URL로 이동합니다.
         */
        if (redirectRequired || containsRemovedPageSizeParameter(request)) {
            addCanonicalPageAttributes(redirectAttributes, query);
            return AdminRoutes.ADMIN_HOME;
        }

        return "admin/dashboard";
    }

    private boolean containsRemovedPageSizeParameter(HttpServletRequest request) {
        return REMOVED_PAGE_SIZE_PARAMETERS.stream()
                .anyMatch(parameterName -> request.getParameter(parameterName) != null);
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
            }
            case CHATS -> {
                redirectAttributes.addAttribute("chatStatus", query.getChatStatus());
                redirectAttributes.addAttribute("chatCategory", query.getChatCategory());
                redirectAttributes.addAttribute("chatKeyword", query.getChatKeyword());
                redirectAttributes.addAttribute("chatPage", query.getChatPage());
            }
            case BOARDS -> {
                redirectAttributes.addAttribute("boardCategory", query.getBoardCategory());
                redirectAttributes.addAttribute("boardKeyword", query.getBoardKeyword());
                redirectAttributes.addAttribute("boardPage", query.getBoardPage());
            }
            case VISITS -> {
                redirectAttributes.addAttribute("visitKeyword", query.getVisitKeyword());
                redirectAttributes.addAttribute("visitPage", query.getVisitPage());
            }
            default -> {
                // 목록 화면이 아닌 경우 별도 페이지 파라미터가 없습니다.
            }
        }
    }
}
