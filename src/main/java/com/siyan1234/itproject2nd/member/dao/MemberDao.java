package com.siyan1234.itproject2nd.member.dao;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberDao {

    MemberDto findByMemberId(@Param("memberId") String memberId); // 로그인 아이디

    MemberDto findByEmail(@Param("email") String email); // 이메일 중복 확인용

    int signup(SignupDto signupDto); // 회원가입 INSERT 실행 후 영향받은 행 수를 반환

    List<MemberDto> findAllMembers(); // 관리자 회원 목록 조회용 SELECT 연결
}
