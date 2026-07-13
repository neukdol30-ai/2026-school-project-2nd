package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 콘솔 Controller입니다.
 *
 * 이번 구조의 핵심:
 * - /admin 하나의 관리자 콘솔 안에서 대시보드/상담관리/회원관리를 전환합니다.
 * - /admin/chats, /admin/members는 별도 화면으로 이동하지 않고 /admin?view=... 로 연결합니다.
 * - 관리자 프로필은 /admin/members/{no}로 이동하지 않고 /admin 화면 안의 모달에서 확인합니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final int DEFAULT_MEMBER_SIZE = 10;
    private static final int DEFAULT_CHAT_SIZE = 10;

    private final MemberService memberService;
    private final AdminDashboardService adminDashboardService;
    private final ChatService chatService;
    private final ChatRedisService chatRedisService;

    /**
     * 관리자 로그인 화면
     *
     * /admin 주소로 직접 접근했을 때 로그인하지 않은 사용자는
     * SecurityConfig의 AuthenticationEntryPoint에 의해 /admin/login으로 이동합니다.
     */
    @GetMapping("/login")
    public String adminLogin(@AuthenticationPrincipal CustomUserDetails customUserDetails, Model model) {
        MemberDto loginUser = getLoginMember(customUserDetails);

        if (isAdmin(loginUser)) {
            return "redirect:/admin";
        }

        model.addAttribute("loginUser", loginUser);
        return "admin/login";
    }

    /**
     * 관리자 단일 콘솔 화면입니다.
     * view 값에 따라 대시보드/상담관리/회원관리 영역이 같은 페이지 안에서 전환됩니다.
     */
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
        MemberDto loginAdmin = getLoginMember(customUserDetails);
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        memberPage = normalizePage(memberPage);
        chatPage = normalizePage(chatPage);
        memberSize = normalizeSize(memberSize, DEFAULT_MEMBER_SIZE);
        chatSize = normalizeSize(chatSize, DEFAULT_CHAT_SIZE);

        String cleanMemberKeyword = cleanText(memberKeyword);
        String cleanChatStatus = cleanText(chatStatus);
        String cleanChatCategory = cleanText(chatCategory);
        String cleanChatKeyword = cleanText(chatKeyword);

        List<MemberDto> adminMemberList = adminDashboardService.findAdminMembers(cleanMemberKeyword, memberPage, memberSize);
        long memberTotalCount = adminDashboardService.countAdminMembers(cleanMemberKeyword);
        int memberTotalPages = calculateTotalPages(memberTotalCount, memberSize);

        List<RecentChatRoomDto> adminChatRoomList = adminDashboardService.findAdminChatRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword,
                chatPage,
                chatSize
        );
        long chatTotalCount = adminDashboardService.countAdminChatRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword
        );
        int chatTotalPages = calculateTotalPages(chatTotalCount, chatSize);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("loginAdmin", loginAdmin);
        model.addAttribute("activeView", activeView);

        model.addAttribute("adminMemberList", adminMemberList);
        model.addAttribute("memberKeyword", cleanMemberKeyword);
        model.addAttribute("memberPage", memberPage);
        model.addAttribute("memberSize", memberSize);
        model.addAttribute("memberTotalCount", memberTotalCount);
        model.addAttribute("memberTotalPages", memberTotalPages);

        model.addAttribute("adminChatRoomList", adminChatRoomList);
        model.addAttribute("chatStatus", cleanChatStatus);
        model.addAttribute("chatCategory", cleanChatCategory);
        model.addAttribute("chatKeyword", cleanChatKeyword);
        model.addAttribute("chatPage", chatPage);
        model.addAttribute("chatSize", chatSize);
        model.addAttribute("chatTotalCount", chatTotalCount);
        model.addAttribute("chatTotalPages", chatTotalPages);

        return "admin/dashboard";
    }

    /**
     * 관리자 URL 기준 상담 관리입니다.
     * 별도 상담 관리 화면으로 이동하지 않고 /admin 단일 콘솔의 상담관리 탭으로 연결합니다.
     */
    @GetMapping("/chats")
    public String adminChats() {
        return "redirect:/admin?view=chats";
    }

    /**
     * 기존 상세 상담방은 다음 단계에서 관리자 콘솔 내부 상세 패널로 통합 예정입니다.
     * 현재는 기존 안정화된 상담 상세 화면을 유지합니다.
     */
    @GetMapping("/chats/{roomNo}")
    public String adminChatRoom(@PathVariable("roomNo") Integer roomNo) {
        return "redirect:/chat/admin/" + roomNo;
    }


    /**
     * 관리자 콘솔 상담 단건 삭제입니다.
     * 진행 중인 상담은 실수 방지를 위해 삭제하지 않고, 종료된 상담만 삭제합니다.
     */
    @PostMapping("/chats/{roomNo}/delete")
    public String deleteChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            RedirectAttributes redirectAttributes
    ) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if (!"CLOSED".equals(chatRoom.getStatus())) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "진행 중 상담은 먼저 종료한 뒤 삭제할 수 있습니다.");
            return "redirect:/admin?view=chats";
        }

        chatRedisService.deleteMessages(roomNo);
        chatService.deleteRoom(roomNo);

        redirectAttributes.addFlashAttribute("adminMessage", "종료된 상담방 #" + roomNo + "번을 삭제했습니다.");
        return "redirect:/admin?view=chats";
    }

    /**
     * 관리자 콘솔 상담 선택 삭제입니다.
     * 선택된 상담 중 CLOSED 상태만 삭제하고 OPEN 상태는 건너뜁니다.
     */
    @PostMapping("/chats/delete")
    public String deleteSelectedChatRooms(
            @RequestParam(value = "roomNoList", required = false) List<Integer> roomNoList,
            RedirectAttributes redirectAttributes
    ) {
        if (roomNoList == null || roomNoList.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 상담방을 선택해 주세요.");
            return "redirect:/admin?view=chats";
        }

        int deletedCount = 0;
        int skippedCount = 0;

        for (Integer roomNo : roomNoList) {
            ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

            if (chatRoom == null || !"CLOSED".equals(chatRoom.getStatus())) {
                skippedCount++;
                continue;
            }

            chatRedisService.deleteMessages(roomNo);
            chatService.deleteRoom(roomNo);
            deletedCount++;
        }

        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제 가능한 종료 상담이 없습니다. 진행 중 상담은 종료 후 삭제해 주세요.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "종료 상담 " + deletedCount + "건을 삭제했습니다."
                    + (skippedCount > 0 ? " 진행 중이거나 없는 상담 " + skippedCount + "건은 제외했습니다." : ""));
        }

        return "redirect:/admin?view=chats";
    }

    /** 회원 관리는 /admin 단일 콘솔의 회원관리 탭으로 연결합니다. */
    @GetMapping("/members")
    public String memberList() {
        return "redirect:/admin?view=members";
    }



    /**
     * 관리자 콘솔 회원 단건 삭제입니다.
     * 현재 로그인 중인 관리자 본인 계정은 삭제하지 않습니다.
     */
    @PostMapping("/members/{no}/delete")
    public String deleteMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = getLoginMember(customUserDetails);

        if (loginAdmin != null && loginAdmin.getNo() != null && loginAdmin.getNo().equals(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "현재 로그인 중인 관리자 본인 계정은 삭제할 수 없습니다.");
            return "redirect:/admin?view=members";
        }

        int deletedCount = memberService.deleteMember(no);

        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 회원을 찾을 수 없습니다.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "회원 #" + no + "번을 삭제했습니다.");
        }

        return "redirect:/admin?view=members";
    }

    /**
     * 관리자 콘솔 회원 선택 삭제입니다.
     * 현재 로그인 중인 관리자 본인 계정은 선택되어 있어도 제외합니다.
     */
    @PostMapping("/members/delete")
    public String deleteSelectedMembers(
            @RequestParam(value = "memberNoList", required = false) List<Integer> memberNoList,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        if (memberNoList == null || memberNoList.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 회원을 선택해 주세요.");
            return "redirect:/admin?view=members";
        }

        MemberDto loginAdmin = getLoginMember(customUserDetails);
        Integer loginAdminNo = loginAdmin == null ? null : loginAdmin.getNo();
        int requestedCount = memberNoList.size();
        int deletedCount = memberService.deleteMembers(memberNoList, loginAdminNo);
        int skippedCount = requestedCount - deletedCount;

        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제된 회원이 없습니다. 현재 로그인 중인 관리자 본인은 삭제할 수 없습니다.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "회원 " + deletedCount + "명을 삭제했습니다."
                    + (skippedCount > 0 ? " 제외된 항목 " + skippedCount + "건이 있습니다." : ""));
        }

        return "redirect:/admin?view=members";
    }


    /** 기존 상세 URL은 팀원/기존 코드 호환을 위해 유지합니다. */
    @GetMapping("/members/{no}")
    public String memberDetail(@PathVariable("no") Integer no, RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("view", "members");
        redirectAttributes.addAttribute("focusMemberNo", no);
        return "redirect:/admin";
    }

    @GetMapping("/members/{no}/edit") // {no} : 주소 안 변수
    public String memberEditForm(@PathVariable("no") Integer no, Model model) {
        MemberDto member = memberService.findByNo(no);
        if (member == null) {
            return "redirect:/admin?view=members";
        }
        model.addAttribute("member", member);
        return "admin/member-edit";
    }

    @PostMapping("/members/{no}/edit")
    public String memberEditUpdate(@PathVariable("no") Integer no,
                                   @ModelAttribute("member") MemberDto member) {
        member.setNo(no);
        memberService.updateMember(member);
        return "redirect:/admin?view=members";
    }

    /**
     * Spring Security Principal에서 로그인 회원 정보를 꺼냅니다.
     */
    private MemberDto getLoginMember(CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return null;
        }

        return customUserDetails.getMemberDto();
    }

    /**
     * 관리자 권한 여부를 확인합니다.
     */
    private boolean isAdmin(MemberDto memberDto) {
        return memberDto != null && "ADMIN".equals(memberDto.getRole());
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
