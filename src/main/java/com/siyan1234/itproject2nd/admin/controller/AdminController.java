package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final MemberService memberService;
    private final AdminDashboardService adminDashboardService;

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
     * 관리자 메인 대시보드
     *
     * 기존 admin/main.html 대신 운영 현황 요약이 먼저 보이는 dashboard.html을 사용합니다.
     * 로그인한 관리자 정보를 함께 전달하여 관리자 프로필 카드에 표시합니다.
     */
    @GetMapping({"", "/", "/dashboard"})
    public String adminDashboard(
            Model model,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();
        MemberDto loginAdmin = getLoginMember(customUserDetails);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("loginAdmin", loginAdmin);

        return "admin/dashboard";
    }

    /**
     * 관리자 URL 기준 상담 관리 진입점입니다.
     * 실제 상담 목록 기능은 기존 /chat/admin 화면을 재사용합니다.
     */
    @GetMapping("/chats")
    public String adminChats() {
        return "redirect:/chat/admin";
    }

    /**
     * 관리자 URL 기준 상담 상세 진입점입니다.
     * 실제 상담 상세 기능은 기존 /chat/admin/{roomNo} 화면을 재사용합니다.
     */
    @GetMapping("/chats/{roomNo}")
    public String adminChatRoom(@PathVariable("roomNo") Integer roomNo) {
        return "redirect:/chat/admin/" + roomNo;
    }

    @GetMapping("/members")
    public String memberList(Model model) {

        List<MemberDto> memberList = memberService.findAllMembers();

        model.addAttribute("memberList", memberList);

        return "admin/member-list";
    }

    // 관리자 회원 상세보기 화면 / GET /admin/members/{no}
    @GetMapping("/members/{no}")
    public String memberDetail(@PathVariable("no") Integer no, Model model) {

        MemberDto member = memberService.findByNo(no); // no로 회원 한 명 조회.

        if (member == null) {
            return "redirect:/admin/members";
        }

        model.addAttribute("member", member); // 조회한 회원을 화면으로 전달(이름 "member")

        return "admin/member-detail"; // templates/admin/member-detail.html
    }

    @GetMapping("/members/{no}/edit") // {no} : 주소 안 변수
    public String memberEditForm(@PathVariable("no") Integer no, Model model) {
        MemberDto member = memberService.findByNo(no);
        if (member == null) {
            return "redirect:/admin/members";
        }
        model.addAttribute("member", member);
        return "admin/member-edit";
    }

    @PostMapping("/members/{no}/edit")
    public String memberEditUpdate(@PathVariable("no") Integer no,
                                   @ModelAttribute("member") MemberDto member) {
        member.setNo(no);
        memberService.updateMember(member);
        return "redirect:/admin/members/" + no;
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
}
