package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

/** 회원 탈퇴 요청 DTO입니다. */
@Getter
@Setter
public class MyPageWithdrawDto {

    private String password;
    private String confirmText;
    private String reason;
}
