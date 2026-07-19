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

    MemberDto findByEmail(@Param("email") String email); // 이메일 중복 확인용 + 소셜 계정 연동 판별용

    MemberDto findByNickname(@Param("nickname") String nickname); // 닉네임 중복 확인용 + 소셜 닉네임 중복 회피용

    int signup(SignupDto signupDto); // 일반 회원가입 INSERT 실행 후 영향 받은 행 수를 반환

    // 소셜 로그인으로 처음 들어온 회원을 member 테이블 저장 / MemberMapper.xml의 <insert id="insertSocialMember">와 이름 일치해야 함.
    int insertSocialMember(MemberDto memberDto);

    List<MemberDto> findAllMembers(); // 관리자 회원 목록 조회용 SELECT 연결

    MemberDto findByNo(@Param("no") Integer no); // 관리자 회원 상세 조회용 SELECT 연결 (회원 고유번호 no 기준)

    int updateMember(MemberDto memberDto); // 관리자 회원 정보 수정용 UPDATE 연결.

    /** 관리자 회원 삭제용 DELETE 연결 */
    int deleteMember(@Param("no") Integer no); // 영준

    // 아이디, 비밀번호 찾기 재작업
    // 비밀번호 찾기 : 사용자가 입력한 아이디 + 이메일이 같은 회원의 것인지 본인 확인용
    MemberDto findByMemberIdAndEmail(@Param("memberId") String memberId, @Param("email") String email);

    // 비밀번호 재설정 : 이메일을 기준으로 새 비밀번호(이미 암호화된 값)를 저장
    int updatePasswordByEmail(@Param("email") String email, @Param("password") String password);
}
