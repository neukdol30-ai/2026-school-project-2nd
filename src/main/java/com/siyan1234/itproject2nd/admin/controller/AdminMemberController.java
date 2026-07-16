package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
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
        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/role/admin")
    public String grantAdminRole(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        int updatedCount = adminMemberService.grantAdmin(no, loginAdminNo);

        if (isSelf(no, loginAdminNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_SELF_DENIED);
        } else if (adminMemberService.isBanned(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_BANNED_DENIED);
        } else if (updatedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_CHANGE_FAILED);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.memberPromoted(no));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/role/user")
    public String grantUserRole(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        int updatedCount = adminMemberService.grantUser(no, loginAdminNo);

        if (isSelf(no, loginAdminNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_SELF_DENIED);
        } else if (adminMemberService.isLastActiveAdmin(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_LAST_ADMIN_DENIED);
        } else if (updatedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_ROLE_CHANGE_FAILED);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.memberDemoted(no));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/ban")
    public String banMember(
            @PathVariable("no") Integer no,
            @RequestParam(value = "banReason", required = false) String banReason,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        int updatedCount = adminMemberService.banMember(no, banReason, loginAdminNo);

        if (isSelf(no, loginAdminNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_BAN_SELF_DENIED);
        } else if (adminMemberService.isAdminAccount(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_BAN_ADMIN_DENIED);
        } else if (updatedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_BAN_FAILED);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.memberBanned(no));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/unban")
    public String unbanMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        int updatedCount = adminMemberService.unbanMember(no, loginAdminNo);

        if (updatedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_UNBAN_FAILED);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.memberUnbanned(no));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/delete")
    public String deleteMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        int deletedCount = adminMemberService.deleteMember(no, loginAdminNo);

        if (isSelf(no, loginAdminNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_DELETE_SELF_DENIED);
        } else if (adminMemberService.isAdminAccount(no)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_DELETE_ADMIN_DENIED);
        } else if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_DELETE_NOT_FOUND);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.memberDeleted(no));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/delete")
    public String deleteSelectedMembers(
            @RequestParam(value = "memberNoList", required = false) List<Integer> memberNoList,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        Integer loginAdminNo = resolveLoginAdminNo(customUserDetails);
        AdminDeleteResultDto result = adminMemberService.deleteMembers(memberNoList, loginAdminNo);

        if (result.getRequestedCount() == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_DELETE_NOT_SELECTED);
            return AdminRoutes.ADMIN_MEMBERS;
        }

        if (!result.hasDeletedItem()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.MEMBER_DELETE_NO_RESULT);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.selectedMembersDeleted(result));
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @GetMapping("/{no}")
    public String memberDetail(
            @PathVariable("no") Integer no,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("view", "members");
        redirectAttributes.addAttribute("focusMemberNo", no);
        return AdminRoutes.ADMIN_HOME;
    }

    @GetMapping("/{no}/edit")
    public String memberEditForm(
            @PathVariable("no") Integer no,
            Model model
    ) {
        MemberDto member = adminMemberService.findByNo(no);

        if (member == null) {
            return AdminRoutes.ADMIN_MEMBERS;
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
        return AdminRoutes.ADMIN_MEMBERS;
    }

    private Integer resolveLoginAdminNo(CustomUserDetails customUserDetails) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        return loginAdmin == null ? null : loginAdmin.getNo();
    }

    private boolean isSelf(Integer memberNo, Integer loginAdminNo) {
        return memberNo != null && loginAdminNo != null && memberNo.equals(loginAdminNo);
    }
}
