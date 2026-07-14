package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
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
 * 담당 범위:
 * - /admin 대시보드 화면 렌더링
 * - /admin?view=chats, /admin?view=members, /admin?view=services 화면 데이터 조립
 *
 * 상담 상세/삭제, 회원 삭제/수정, 로그인 처리는 별도 Controller로 분리했습니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final int DEFAULT_MEMBER_SIZE = 10;
    private static final int DEFAULT_CHAT_SIZE = 10;

    private final AdminDashboardService adminDashboardService;
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

        memberPage = normalizePage(memberPage);
        chatPage = normalizePage(chatPage);
        memberSize = normalizeSize(memberSize, DEFAULT_MEMBER_SIZE);
        chatSize = normalizeSize(chatSize, DEFAULT_CHAT_SIZE);

        String cleanMemberKeyword = cleanText(memberKeyword);
        String cleanChatStatus = cleanText(chatStatus);
        String cleanChatCategory = cleanText(chatCategory);
        String cleanChatKeyword = cleanText(chatKeyword);

        List<MemberDto> adminMemberList = adminDashboardService.findAdminMembers(
                cleanMemberKeyword,
                memberPage,
                memberSize
        );
        long memberTotalCount = adminDashboardService.countAdminMembers(cleanMemberKeyword);

        List<RecentChatRoomDto> adminChatRoomList = adminDashboardService.findAdminChatRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword,
                loginAdmin == null ? null : loginAdmin.getNo(),
                chatPage,
                chatSize
        );
        long chatTotalCount = adminDashboardService.countAdminChatRooms(
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
        model.addAttribute("memberTotalPages", calculateTotalPages(memberTotalCount, memberSize));

        model.addAttribute("adminChatRoomList", adminChatRoomList);
        model.addAttribute("chatStatus", cleanChatStatus);
        model.addAttribute("chatCategory", cleanChatCategory);
        model.addAttribute("chatKeyword", cleanChatKeyword);
        model.addAttribute("chatPage", chatPage);
        model.addAttribute("chatSize", chatSize);
        model.addAttribute("chatTotalCount", chatTotalCount);
        model.addAttribute("chatTotalPages", calculateTotalPages(chatTotalCount, chatSize));

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

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size, int defaultSize) {
        if (size < 1) {
            return defaultSize;
        }

        return Math.min(size, 50);
    }

    private int calculateTotalPages(long totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalCount / size);
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
