package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * /mypage/me 응답 DTO입니다.
 * 메인 로그인 위젯과 마이페이지 모달이 같은 응답을 사용하므로,
 * 로그인 여부와 프로필/활동/최근 내역을 한 번에 담습니다.
 */
@Getter
@Setter
public class MyPageResponseDto {

    private boolean loggedIn;
    private MyPageProfileDto profile;
    private MyPageActivityDto activity;
    private List<MyPageRecentBoardDto> recentBoards;
    private List<MyPageRecentChatDto> recentChats;

    /** 비로그인 사용자 응답입니다. */
    public static MyPageResponseDto anonymous() {
        MyPageResponseDto responseDto = new MyPageResponseDto();
        responseDto.setLoggedIn(false);
        return responseDto;
    }

    /** 로그인 사용자 응답입니다. */
    public static MyPageResponseDto loggedIn(
            MyPageProfileDto profile,
            MyPageActivityDto activity,
            List<MyPageRecentBoardDto> recentBoards,
            List<MyPageRecentChatDto> recentChats
    ) {
        MyPageResponseDto responseDto = new MyPageResponseDto();
        responseDto.setLoggedIn(true);
        responseDto.setProfile(profile);
        responseDto.setActivity(activity);
        responseDto.setRecentBoards(recentBoards);
        responseDto.setRecentChats(recentChats);
        return responseDto;
    }
}
