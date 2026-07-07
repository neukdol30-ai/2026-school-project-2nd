package com.siyan1234.itproject2nd.member.dao;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import org.apache.ibatis.annotations.Param;

// @Mapper 어노테이션 없는 이유: Application의 @MapperScan이 이 dao를 스캔함. -> 불필요.
public interface MemberDao {

    MemberDto findByMemberId(@Param("memberId") String memberId);
}
