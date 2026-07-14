package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** 관리자 콘솔 회원 관리 Controller입니다. */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/members")
public class AdminMemberController {

    private final AdminMemberService adminMemberService;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping
    public String memberList() {
        return "redirect:/admin?view=members";
    }

    @PostMapping("/{no}/delete")
    public String deleteMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        Integer loginAdminNo = loginAdmin == null ? null : loginAdmin.getNo();
        int deletedCount = adminMemberService.deleteMember(no, loginAdminNo);

        if (loginAdminNo != null && loginAdminNo.equals(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "현재 로그인 중인 관리자 본인 계정은 삭제할 수 없습니다.");
        } else if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 회원을 찾을 수 없습니다.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "회원 #" + no + "번을 삭제했습니다.");
        }

        return "redirect:/admin?view=members";
    }

    @PostMapping("/delete")
    public String deleteSelectedMembers(
            @RequestParam(value = "memberNoList", required = false) List<Integer> memberNoList,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        Integer loginAdminNo = loginAdmin == null ? null : loginAdmin.getNo();
        AdminDeleteResultDto result = adminMemberService.deleteMembers(memberNoList, loginAdminNo);

        if (result.getRequestedCount() == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 회원을 선택해 주세요.");
            return "redirect:/admin?view=members";
        }

        if (!result.hasDeletedItem()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제된 회원이 없습니다. 현재 로그인 중인 관리자 본인은 삭제할 수 없습니다.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "회원 " + result.getDeletedCount() + "명을 삭제했습니다."
                    + (result.getSkippedCount() > 0 ? " 제외된 항목 " + result.getSkippedCount() + "건이 있습니다." : ""));
        }

        return "redirect:/admin?view=members";
    }

    @GetMapping("/{no}")
    public String memberDetail(
            @PathVariable("no") Integer no,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("view", "members");
        redirectAttributes.addAttribute("focusMemberNo", no);
        return "redirect:/admin";
    }

    @GetMapping("/{no}/edit")
    public String memberEditForm(
            @PathVariable("no") Integer no,
            Model model
    ) {
        MemberDto member = adminMemberService.findByNo(no);

        if (member == null) {
            return "redirect:/admin?view=members";
        }

        model.addAttribute("member", member);
        return "admin/member-edit";
    }

    @PostMapping("/{no}/edit")
    public String memberEditUpdate(
            @PathVariable("no") Integer no,
            @ModelAttribute("member") MemberDto member
    ) {
        member.setNo(no);
        adminMemberService.updateMember(member);
        return "redirect:/admin?view=members";
    }
}
