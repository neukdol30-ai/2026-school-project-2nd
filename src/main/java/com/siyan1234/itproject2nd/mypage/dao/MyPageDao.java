package com.siyan1234.itproject2nd.mypage.dao;

import com.siyan1234.itproject2nd.mypage.dto.MyPageActivityDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageProfileDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageRecentBoardDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageRecentChatDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 마이페이지 전용 MyBatis DAO입니다.
 *
 * 회원 기본 조회는 member DAO와 일부 겹치지만,
 * 마이페이지 화면에 필요한 활동 통계/최근 내역/social_account 조인 정보는 여기에서 관리합니다.
 */
@Mapper
public interface MyPageDao {

    /** 마이페이지 프로필과 소셜 연결 정보를 조회합니다. */
    MyPageProfileDto findProfileByNo(@Param("memberNo") Integer memberNo);

    /** 내 정보 탭의 이름/닉네임 수정입니다. */
    int updateBasicProfile(@Param("memberNo") Integer memberNo,
                           @Param("profile") MyPageUpdateDto profile);

    /** 보안 설정 탭의 개인정보 수정입니다. */
    int updateSecurityProfile(@Param("memberNo") Integer memberNo,
                              @Param("profile") MyPageUpdateDto profile);

    /** 비밀번호 변경입니다. password는 반드시 암호화된 값이어야 합니다. */
    int updatePassword(@Param("memberNo") Integer memberNo,
                       @Param("password") String password);

    /** 마이페이지 회원 탈퇴입니다. SQL에서 USER 계정만 삭제되도록 한 번 더 제한합니다. */
    int deleteMe(@Param("memberNo") Integer memberNo);

    /** 본인을 제외한 닉네임 중복 수입니다. */
    int countNicknameDuplicateExceptMe(@Param("memberNo") Integer memberNo,
                                       @Param("nickname") String nickname);

    /** 본인을 제외한 이메일 중복 수입니다. */
    int countEmailDuplicateExceptMe(@Param("memberNo") Integer memberNo,
                                    @Param("email") String email);

    /** 마이페이지 활동 요약 카운트입니다. */
    MyPageActivityDto findActivityByMemberNo(@Param("memberNo") Integer memberNo);

    /** 최근 게시글 3개입니다. */
    List<MyPageRecentBoardDto> findRecentBoards(@Param("memberNo") Integer memberNo);

    /** 최근 상담방 3개입니다. */
    List<MyPageRecentChatDto> findRecentChats(@Param("memberNo") Integer memberNo);
}
