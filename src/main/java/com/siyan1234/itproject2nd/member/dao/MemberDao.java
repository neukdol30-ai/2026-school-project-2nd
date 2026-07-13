package com.siyan1234.itproject2nd.member.dao;

import com.siyan1234.itproject2nd.member.dto.LoginDto;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberDao {

    MemberDto findByMemberId(@Param("memberId") String memberId); // 로그인 아이디

    MemberDto findByEmail(@Param("email") String email); // 이메일 중복 확인용

    MemberDto findByNickname(@Param("nickname") String nickname); // 닉네임 중복 확인용

    int signup(SignupDto signupDto); // 회원가입 INSERT 실행 후 영향받은 행 수를 반환

    List<MemberDto> findAllMembers(); // 관리자 회원 목록 조회용 SELECT 연결

    MemberDto findByNo(@Param("no") Integer no); // 관리자 회원 상세 조회용 SELECT 연결 (회원 고유번호 no 기준)

    int updateMember(MemberDto memberDto); // 관리자 회원 정보 수정용 UPDATE 연결.

    /** 관리자 회원 삭제용 DELETE 연결 */
    int deleteMember(@Param("no") Integer no);
}
