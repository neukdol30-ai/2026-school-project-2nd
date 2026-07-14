package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 콘솔 회원 관리 조회용 MyBatis Mapper입니다. */
@Mapper
public interface AdminMemberDao {

    List<MemberDto> findAdminMembers(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countAdminMembers(@Param("keyword") String keyword);
}
