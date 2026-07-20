package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordChangeDto {

    private String currentPassword;
    private String newPassword;
    private String newPasswordCheck;
}
