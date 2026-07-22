package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

/** 비밀번호 변경 요청 DTO입니다. */
@Getter
@Setter
public class PasswordChangeDto {

    private String currentPassword;
    private String newPassword;
    private String newPasswordCheck;
}
