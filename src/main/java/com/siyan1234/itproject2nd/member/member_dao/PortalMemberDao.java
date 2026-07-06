package com.siyan1234.itproject2nd.member.member_dao;

import com.siyan1234.itproject2nd.member.member_dto.PortalMemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PortalMemberDao {

    // 회원가입 INSERT
    int signup(PortalMemberDto portalMemberDto);

    // 아이디 중복 체크
    int countByMemberId(@Param("memberId") String memberId);

    // 이메일 중복 체크
    int countByEmail(@Param("email") String email);
}