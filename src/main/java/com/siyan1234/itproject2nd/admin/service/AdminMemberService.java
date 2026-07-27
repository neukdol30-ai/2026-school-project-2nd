package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminMemberDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
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
    public List<MemberDto> findMembers(String keyword, int page, int size) {
        int offset = AdminPagingHelper.calculateOffset(page, size);
        return adminMemberDao.findAdminMembers(AdminPagingHelper.cleanText(keyword), offset, size);
    }

    @Transactional(readOnly = true)
    public long countMembers(String keyword) {
        Long count = adminMemberDao.countAdminMembers(AdminPagingHelper.cleanText(keyword));
        return count == null ? 0L : count;
    }

    @Transactional(readOnly = true)
    public MemberDto findByNo(Integer memberNo) {
        return memberService.findByNo(memberNo);
    }

    @Transactional
    public void updateMember(MemberDto memberDto) {
        memberService.updateMember(memberDto);
    }

    @Transactional
    public int grantAdmin(Integer memberNo, Integer loginAdminNo) {
        if (!isActiveAdmin(loginAdminNo) || isBanned(memberNo)) {
            return 0;
        }

        return updateRoleSafely(memberNo, loginAdminNo, ROLE_ADMIN);
    }

    /** 마지막 활성 관리자 계정은 USER로 강등하지 않아 관리자 콘솔 접근 수단을 보존합니다. */
    @Transactional
    public int grantUser(Integer memberNo, Integer loginAdminNo) {
        if (!isActiveAdmin(loginAdminNo) || isLastActiveAdmin(memberNo)) {
            return 0;
        }

        return updateRoleSafely(memberNo, loginAdminNo, ROLE_USER);
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
        MemberDto member = findByNo(memberNo);
        return member != null && ROLE_ADMIN.equalsIgnoreCase(member.getRole());
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
        if (memberNo == null || !isActiveAdmin(memberNo)) {
            return false;
        }

        return adminMemberDao.countActiveAdminsExcept(memberNo) == 0;
    }

    /** 관리자 본인과 다른 관리자 계정은 직접 정지할 수 없도록 서비스 계층에서도 방어합니다. */
    @Transactional
    public int banMember(Integer memberNo, String banReason, Integer loginAdminNo) {
        if (!isActiveAdmin(loginAdminNo)
                || isSelf(memberNo, loginAdminNo)
                || memberNo == null
                || isAdminAccount(memberNo)) {
            return 0;
        }

        return adminMemberDao.banMember(memberNo, normalizeBanReason(banReason), loginAdminNo);
    }

    @Transactional
    public int unbanMember(Integer memberNo, Integer loginAdminNo) {
        if (memberNo == null || !isActiveAdmin(loginAdminNo)) {
            return 0;
        }

        return adminMemberDao.unbanMember(memberNo);
    }

    @Transactional
    public int deleteMember(Integer memberNo, Integer loginAdminNo) {
        if (memberNo == null || !isActiveAdmin(loginAdminNo)) {
            return 0;
        }

        if (isSelf(memberNo, loginAdminNo) || isAdminAccount(memberNo)) {
            return 0;
        }

        return memberService.deleteMember(memberNo);
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
            deletedCount += deleteMember(memberNo, loginAdminNo);
        }

        int skippedCount = requestedCount - deletedCount;
        return new AdminDeleteResultDto(requestedCount, deletedCount, skippedCount);
    }

    private int updateRoleSafely(Integer memberNo, Integer loginAdminNo, String role) {
        if (memberNo == null || isSelf(memberNo, loginAdminNo)) {
            return 0;
        }

        return adminMemberDao.updateRole(memberNo, role);
    }

    private boolean isSelf(Integer memberNo, Integer loginAdminNo) {
        return memberNo != null && loginAdminNo != null && memberNo.equals(loginAdminNo);
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
