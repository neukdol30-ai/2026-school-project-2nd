package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MyPageResponseDto {

    private boolean loggedIn;
    private MyPageProfileDto profile;
    private MyPageActivityDto activity;
    private List<MyPageRecentBoardDto> recentBoards;
    private List<MyPageRecentChatDto> recentChats;

    public static MyPageResponseDto anonymous() {
        MyPageResponseDto responseDto = new MyPageResponseDto();
        responseDto.setLoggedIn(false);
        return responseDto;
    }
}
