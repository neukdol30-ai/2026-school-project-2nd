package com.siyan1234.itproject2nd.mypage.dao;

import com.siyan1234.itproject2nd.mypage.dto.MyPageActivityDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageProfileDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageRecentBoardDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageRecentChatDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MyPageDao {

    MyPageProfileDto findProfileByNo(@Param("memberNo") Integer memberNo);

    int updateProfile(@Param("memberNo") Integer memberNo,
                      @Param("profile") MyPageUpdateDto profile);

    int updatePassword(@Param("memberNo") Integer memberNo,
                       @Param("password") String password);

    int deleteMe(@Param("memberNo") Integer memberNo);

    int countNicknameDuplicateExceptMe(@Param("memberNo") Integer memberNo,
                                       @Param("nickname") String nickname);

    int countEmailDuplicateExceptMe(@Param("memberNo") Integer memberNo,
                                    @Param("email") String email);

    MyPageActivityDto findActivityByMemberNo(@Param("memberNo") Integer memberNo);

    List<MyPageRecentBoardDto> findRecentBoards(@Param("memberNo") Integer memberNo);

    List<MyPageRecentChatDto> findRecentChats(@Param("memberNo") Integer memberNo);
}
