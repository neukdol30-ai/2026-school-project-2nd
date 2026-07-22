package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

/*** 마이페이지 POST 요청 공통 응답 DTO입니다., 성공/실패 메시지, 갱신된 마이페이지 데이터, 탈퇴 후 이동할 URL을 공통 형태로 내려줍니다.*/
@Getter
@Setter
public class MyPageActionResponseDto {

    private boolean success;
    private String message;
    private String redirectUrl;
    private MyPageResponseDto myPage;

    /** 성공 후 마이페이지 데이터를 다시 렌더링해야 하는 응답입니다. */
    public static MyPageActionResponseDto success(String message, MyPageResponseDto myPage) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(true);
        responseDto.setMessage(message);
        responseDto.setMyPage(myPage);
        return responseDto;
    }

    /** 성공 후 지정 URL로 이동해야 하는 응답입니다. 예: 회원 탈퇴 후 메인 이동. */
    public static MyPageActionResponseDto successWithRedirect(String message, String redirectUrl) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(true);
        responseDto.setMessage(message);
        responseDto.setRedirectUrl(redirectUrl);
        return responseDto;
    }

    /** 실패 응답입니다. */
    public static MyPageActionResponseDto fail(String message) {
        MyPageActionResponseDto responseDto = new MyPageActionResponseDto();
        responseDto.setSuccess(false);
        responseDto.setMessage(message);
        return responseDto;
    }
}
