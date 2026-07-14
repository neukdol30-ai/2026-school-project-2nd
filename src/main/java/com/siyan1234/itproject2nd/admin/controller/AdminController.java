package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminChatService;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 관리자 단일 콘솔 화면 Controller입니다.
 *
 * 이 Controller는 화면 렌더링에 필요한 Model 조립만 담당합니다.
 * 실제 대시보드/상담/회원 조회 로직은 각 Service로 분리했습니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final int DEFAULT_MEMBER_SIZE = 10;
    private static final int DEFAULT_CHAT_SIZE = 10;

    private final AdminDashboardService adminDashboardService;
    private final AdminMemberService adminMemberService;
    private final AdminChatService adminChatService;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(
            @RequestParam(value = "view", defaultValue = "dashboard") String view,

            @RequestParam(value = "memberKeyword", required = false) String memberKeyword,
            @RequestParam(value = "memberPage", defaultValue = "1") int memberPage,
            @RequestParam(value = "memberSize", defaultValue = "10") int memberSize,

            @RequestParam(value = "chatStatus", required = false) String chatStatus,
            @RequestParam(value = "chatCategory", required = false) String chatCategory,
            @RequestParam(value = "chatKeyword", required = false) String chatKeyword,
            @RequestParam(value = "chatPage", defaultValue = "1") int chatPage,
            @RequestParam(value = "chatSize", defaultValue = "10") int chatSize,

            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        String activeView = normalizeView(view);
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        memberPage = AdminPagingHelper.normalizePage(memberPage);
        chatPage = AdminPagingHelper.normalizePage(chatPage);
        memberSize = AdminPagingHelper.normalizeSize(memberSize, DEFAULT_MEMBER_SIZE);
        chatSize = AdminPagingHelper.normalizeSize(chatSize, DEFAULT_CHAT_SIZE);

        String cleanMemberKeyword = AdminPagingHelper.cleanText(memberKeyword);
        String cleanChatStatus = AdminPagingHelper.cleanText(chatStatus);
        String cleanChatCategory = AdminPagingHelper.cleanText(chatCategory);
        String cleanChatKeyword = AdminPagingHelper.cleanText(chatKeyword);

        List<MemberDto> adminMemberList = adminMemberService.findMembers(
                cleanMemberKeyword,
                memberPage,
                memberSize
        );
        long memberTotalCount = adminMemberService.countMembers(cleanMemberKeyword);

        List<RecentChatRoomDto> adminChatRoomList = adminChatService.findRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword,
                loginAdmin == null ? null : loginAdmin.getNo(),
                chatPage,
                chatSize
        );
        long chatTotalCount = adminChatService.countRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword
        );

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("loginAdmin", loginAdmin);
        model.addAttribute("activeView", activeView);

        model.addAttribute("adminMemberList", adminMemberList);
        model.addAttribute("memberKeyword", cleanMemberKeyword);
        model.addAttribute("memberPage", memberPage);
        model.addAttribute("memberSize", memberSize);
        model.addAttribute("memberTotalCount", memberTotalCount);
        model.addAttribute("memberTotalPages", AdminPagingHelper.calculateTotalPages(memberTotalCount, memberSize));

        model.addAttribute("adminChatRoomList", adminChatRoomList);
        model.addAttribute("chatStatus", cleanChatStatus);
        model.addAttribute("chatCategory", cleanChatCategory);
        model.addAttribute("chatKeyword", cleanChatKeyword);
        model.addAttribute("chatPage", chatPage);
        model.addAttribute("chatSize", chatSize);
        model.addAttribute("chatTotalCount", chatTotalCount);
        model.addAttribute("chatTotalPages", AdminPagingHelper.calculateTotalPages(chatTotalCount, chatSize));

        return "admin/dashboard";
    }

    private String normalizeView(String view) {
        if ("members".equals(view)) {
            return "members";
        }

        if ("chats".equals(view)) {
            return "chats";
        }

        if ("services".equals(view)) {
            return "services";
        }

        return "dashboard";
    }
}
