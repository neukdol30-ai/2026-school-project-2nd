package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminMemberService;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminMemberActionResult;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
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
        AdminMemberActionResult result = adminMemberService.grantAdmin(
                no,
                resolveLoginAdminNo(customUserDetails)
        );

        if (result.isSuccess()) {
            addSuccess(redirectAttributes, AdminFlashMessage.memberPromoted(no));
        } else if (result == AdminMemberActionResult.SELF_ACTION_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_SELF_DENIED);
        } else if (result == AdminMemberActionResult.BANNED_MEMBER_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_BANNED_DENIED);
        } else {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_CHANGE_FAILED);
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/role/user")
    public String grantUserRole(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        AdminMemberActionResult result = adminMemberService.grantUser(
                no,
                resolveLoginAdminNo(customUserDetails)
        );

        if (result.isSuccess()) {
            addSuccess(redirectAttributes, AdminFlashMessage.memberDemoted(no));
        } else if (result == AdminMemberActionResult.SELF_ACTION_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_SELF_DENIED);
        } else if (result == AdminMemberActionResult.LAST_ADMIN_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_LAST_ADMIN_DENIED);
        } else {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_ROLE_CHANGE_FAILED);
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
        AdminMemberActionResult result = adminMemberService.banMember(
                no,
                banReason,
                resolveLoginAdminNo(customUserDetails)
        );

        if (result.isSuccess()) {
            addSuccess(redirectAttributes, AdminFlashMessage.memberBanned(no));
        } else if (result == AdminMemberActionResult.SELF_ACTION_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_BAN_SELF_DENIED);
        } else if (result == AdminMemberActionResult.ADMIN_ACCOUNT_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_BAN_ADMIN_DENIED);
        } else {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_BAN_FAILED);
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/unban")
    public String unbanMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        AdminMemberActionResult result = adminMemberService.unbanMember(
                no,
                resolveLoginAdminNo(customUserDetails)
        );

        if (result.isSuccess()) {
            addSuccess(redirectAttributes, AdminFlashMessage.memberUnbanned(no));
        } else {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_UNBAN_FAILED);
        }

        return AdminRoutes.ADMIN_MEMBERS;
    }

    @PostMapping("/{no}/delete")
    public String deleteMember(
            @PathVariable("no") Integer no,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        AdminMemberActionResult result = adminMemberService.deleteMember(
                no,
                resolveLoginAdminNo(customUserDetails)
        );

        if (result.isSuccess()) {
            addSuccess(redirectAttributes, AdminFlashMessage.memberDeleted(no));
        } else if (result == AdminMemberActionResult.SELF_ACTION_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_DELETE_SELF_DENIED);
        } else if (result == AdminMemberActionResult.ADMIN_ACCOUNT_DENIED) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_DELETE_ADMIN_DENIED);
        } else {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_DELETE_NOT_FOUND);
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
            addError(redirectAttributes, AdminFlashMessage.MEMBER_DELETE_NOT_SELECTED);
            return AdminRoutes.ADMIN_MEMBERS;
        }

        if (!result.hasDeletedItem()) {
            addError(redirectAttributes, AdminFlashMessage.MEMBER_DELETE_NO_RESULT);
        } else {
            addSuccess(redirectAttributes, AdminFlashMessage.selectedMembersDeleted(result));
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
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("view", "memberEdit");
        redirectAttributes.addAttribute("editMemberNo", no);
        return AdminRoutes.ADMIN_HOME;
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

    private void addSuccess(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("adminMessage", message);
    }

    private void addError(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("adminErrorMessage", message);
    }
}
