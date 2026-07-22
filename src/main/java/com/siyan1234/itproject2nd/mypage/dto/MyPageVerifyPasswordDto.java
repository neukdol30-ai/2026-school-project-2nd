package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

/** 보안 설정 진입 전 현재 비밀번호 확인 요청 DTO입니다. */
@Getter
@Setter
public class MyPageVerifyPasswordDto {

    private String currentPassword;
}
