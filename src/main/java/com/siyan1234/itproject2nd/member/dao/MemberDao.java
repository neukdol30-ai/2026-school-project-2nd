package com.siyan1234.itproject2nd.member.dao;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MemberDao {

    MemberDto findByMemberId(@Param("memberId") String memberId); // 로그인 아이디

    MemberDto findByEmail(@Param("email") String email); // 이메일 중복 확인용 + 소셜 계정 연동 판별용

    MemberDto findByNickname(@Param("nickname") String nickname); // 닉네임 중복 확인용 + 소셜 닉네임 중복 회피용

    int signup(SignupDto signupDto); // 일반 회원가입 INSERT 실행 후 영향 받은 행 수를 반환

    // 소셜 로그인으로 처음 들어온 회원을 member 테이블 저장 / MemberMapper.xml의 <insert id="insertSocialMember">와 이름 일치해야 함.
    int insertSocialMember(MemberDto memberDto);

    List<MemberDto> findAllMembers(); // 관리자 회원 목록 조회용 SELECT 연결

    MemberDto findByNo(@Param("no") Integer no); // 관리자 회원 상세 조회용 SELECT 연결 (회원 고유번호 no 기준)

    int updateMember(MemberDto memberDto); // 관리자 회원 정보 수정용 UPDATE 연결.
}
