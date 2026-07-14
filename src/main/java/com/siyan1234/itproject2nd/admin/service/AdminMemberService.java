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
    public int deleteMember(Integer memberNo, Integer loginAdminNo) {
        if (memberNo == null) {
            return 0;
        }

        if (loginAdminNo != null && loginAdminNo.equals(memberNo)) {
            return 0;
        }

        return memberService.deleteMember(memberNo);
    }

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
}
