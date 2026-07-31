package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminMemberDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.support.AdminMemberActionResult;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 회원 관리 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String DEFAULT_BAN_REASON = "관리자에 의해 이용이 제한되었습니다.";

    private final AdminMemberDao adminMemberDao;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<MemberDto> findMembers(String keyword, int page) {
        int offset = AdminPagingHelper.calculateOffset(page);
        return adminMemberDao.findAdminMembers(
                AdminPagingHelper.cleanText(keyword),
                offset,
                AdminPagingHelper.PAGE_SIZE
        );
    }

    @Transactional(readOnly = true)
    public long countMembers(String keyword) {
        Long count = adminMemberDao.countAdminMembers(AdminPagingHelper.cleanText(keyword));
        return count == null ? 0L : count;
    }

    @Transactional(readOnly = true)
    public MemberDto findByNo(Integer memberNo) {
        if (memberNo == null) {
            return null;
        }
        return memberService.findByNo(memberNo);
    }

    @Transactional
    public void updateMember(MemberDto memberDto) {
        memberService.updateMember(memberDto);
    }

    @Transactional
    public AdminMemberActionResult grantAdmin(Integer memberNo, Integer loginAdminNo) {
        TargetValidation validation = validateActorAndTarget(memberNo, loginAdminNo);
        if (validation.failed()) {
            return validation.failure();
        }

        MemberDto targetMember = validation.targetMember();
        if (targetMember.isBanned()) {
            return AdminMemberActionResult.BANNED_MEMBER_DENIED;
        }

        return toUpdateResult(adminMemberDao.updateRole(memberNo, ROLE_ADMIN));
    }

    /** 마지막 활성 관리자 계정은 USER로 강등하지 않아 관리자 콘솔 접근 수단을 보존합니다. */
    @Transactional
    public AdminMemberActionResult grantUser(Integer memberNo, Integer loginAdminNo) {
        TargetValidation validation = validateActorAndTarget(memberNo, loginAdminNo);
        if (validation.failed()) {
            return validation.failure();
        }

        MemberDto targetMember = validation.targetMember();
        if (isLastActiveAdmin(targetMember)) {
            return AdminMemberActionResult.LAST_ADMIN_DENIED;
        }

        return toUpdateResult(adminMemberDao.updateRole(memberNo, ROLE_USER));
    }

    @Transactional(readOnly = true)
    public boolean isBanned(Integer memberNo) {
        if (memberNo == null) {
            return false;
        }
        return adminMemberDao.countBannedMember(memberNo) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isAdminAccount(Integer memberNo) {
        return isAdminAccount(findByNo(memberNo));
    }

    @Transactional(readOnly = true)
    public boolean isActiveAdmin(Integer memberNo) {
        if (memberNo == null) {
            return false;
        }
        return adminMemberDao.countActiveAdminByNo(memberNo) > 0;
    }

    @Transactional(readOnly = true)
    public boolean isLastActiveAdmin(Integer memberNo) {
        if (memberNo == null) {
            return false;
        }
        return isLastActiveAdmin(findByNo(memberNo));
    }

    /** 관리자 본인과 다른 관리자 계정은 직접 정지할 수 없도록 서비스 계층에서 방어합니다. */
    @Transactional
    public AdminMemberActionResult banMember(Integer memberNo, String banReason, Integer loginAdminNo) {
        TargetValidation validation = validateActorAndTarget(memberNo, loginAdminNo);
        if (validation.failed()) {
            return validation.failure();
        }

        MemberDto targetMember = validation.targetMember();
        if (isAdminAccount(targetMember)) {
            return AdminMemberActionResult.ADMIN_ACCOUNT_DENIED;
        }

        int updatedCount = adminMemberDao.banMember(
                memberNo,
                normalizeBanReason(banReason),
                loginAdminNo
        );
        return toUpdateResult(updatedCount);
    }

    @Transactional
    public AdminMemberActionResult unbanMember(Integer memberNo, Integer loginAdminNo) {
        if (!isActiveAdmin(loginAdminNo)) {
            return AdminMemberActionResult.ACTOR_NOT_ALLOWED;
        }

        if (memberNo == null || findByNo(memberNo) == null) {
            return AdminMemberActionResult.TARGET_NOT_FOUND;
        }

        return toUpdateResult(adminMemberDao.unbanMember(memberNo));
    }

    @Transactional
    public AdminMemberActionResult deleteMember(Integer memberNo, Integer loginAdminNo) {
        TargetValidation validation = validateActorAndTarget(memberNo, loginAdminNo);
        if (validation.failed()) {
            return validation.failure();
        }

        MemberDto targetMember = validation.targetMember();
        if (isAdminAccount(targetMember)) {
            return AdminMemberActionResult.ADMIN_ACCOUNT_DENIED;
        }

        return toUpdateResult(memberService.deleteMember(memberNo));
    }

    /** 선택 목록에 본인 또는 관리자 계정이 포함되어도 가능한 회원만 삭제하고 결과를 집계합니다. */
    @Transactional
    public AdminDeleteResultDto deleteMembers(List<Integer> memberNoList, Integer loginAdminNo) {
        if (memberNoList == null || memberNoList.isEmpty()) {
            return new AdminDeleteResultDto(0, 0, 0);
        }

        int deletedCount = 0;
        int requestedCount = memberNoList.size();

        for (Integer memberNo : memberNoList) {
            if (deleteMember(memberNo, loginAdminNo).isSuccess()) {
                deletedCount++;
            }
        }

        int skippedCount = requestedCount - deletedCount;
        return new AdminDeleteResultDto(requestedCount, deletedCount, skippedCount);
    }

    /**
     * 모든 단일 회원 작업에서 공통으로 필요한 관리자 권한·대상 존재·본인 작업 금지 검증입니다.
     * 검증 실패 사유 또는 후속 작업에 사용할 대상 회원을 함께 반환합니다.
     */
    private TargetValidation validateActorAndTarget(Integer memberNo, Integer loginAdminNo) {
        if (!isActiveAdmin(loginAdminNo)) {
            return TargetValidation.failure(AdminMemberActionResult.ACTOR_NOT_ALLOWED);
        }

        if (memberNo == null) {
            return TargetValidation.failure(AdminMemberActionResult.TARGET_NOT_FOUND);
        }

        MemberDto targetMember = findByNo(memberNo);
        if (targetMember == null) {
            return TargetValidation.failure(AdminMemberActionResult.TARGET_NOT_FOUND);
        }

        if (isSelf(memberNo, loginAdminNo)) {
            return TargetValidation.failure(AdminMemberActionResult.SELF_ACTION_DENIED);
        }

        return TargetValidation.success(targetMember);
    }

    private AdminMemberActionResult toUpdateResult(int updatedCount) {
        return updatedCount > 0
                ? AdminMemberActionResult.SUCCESS
                : AdminMemberActionResult.UPDATE_FAILED;
    }

    private boolean isAdminAccount(MemberDto member) {
        return member != null && ROLE_ADMIN.equalsIgnoreCase(member.getRole());
    }

    private boolean isLastActiveAdmin(MemberDto member) {
        if (member == null || !isAdminAccount(member) || member.isBanned()) {
            return false;
        }
        return adminMemberDao.countActiveAdminsExcept(member.getNo()) == 0;
    }

    private boolean isSelf(Integer memberNo, Integer loginAdminNo) {
        return memberNo != null && loginAdminNo != null && memberNo.equals(loginAdminNo);
    }

    private record TargetValidation(AdminMemberActionResult failure, MemberDto targetMember) {

        private static TargetValidation success(MemberDto targetMember) {
            return new TargetValidation(null, targetMember);
        }

        private static TargetValidation failure(AdminMemberActionResult failure) {
            return new TargetValidation(failure, null);
        }

        private boolean failed() {
            return failure != null;
        }
    }

    private String normalizeBanReason(String banReason) {
        if (banReason == null || banReason.isBlank()) {
            return DEFAULT_BAN_REASON;
        }

        String trimmedReason = banReason.trim();
        if (trimmedReason.length() > 500) {
            return trimmedReason.substring(0, 500);
        }
        return trimmedReason;
    }
}
