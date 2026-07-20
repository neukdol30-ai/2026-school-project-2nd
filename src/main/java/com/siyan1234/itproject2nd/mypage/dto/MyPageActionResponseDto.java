package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MyPageActionResponseDto {

    private boolean success;
    private String message;
    private String redirectUrl;
    private MyPageResponseDto myPage;

    public static MyPageActionResponseDto success(String message, MyPageResponseDto myPage) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(true);
        responseDto.setMessage(message);
        responseDto.setMyPage(myPage);
        return responseDto;
    }

    public static MyPageActionResponseDto successWithRedirect(String message, String redirectUrl) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(true);
        responseDto.setMessage(message);
        responseDto.setRedirectUrl(redirectUrl);
        return responseDto;
    }

    public static MyPageActionResponseDto fail(String message) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(false);
        responseDto.setMessage(message);
        return responseDto;
    }
}
