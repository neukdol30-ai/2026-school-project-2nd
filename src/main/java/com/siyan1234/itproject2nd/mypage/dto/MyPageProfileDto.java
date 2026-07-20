package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class MyPageProfileDto {

    private Integer no;
    private String memberId;
    private String name;
    private String nickname;
    private String email;
    private String phone;
    private LocalDate birthDate;
    private String gender;
    private String postcode;
    private String address;
    private String detailAddress;
    private String role;
    private String profileImage;
    private LocalDateTime lastLoginDate;
    private LocalDateTime regdate;

    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }

        if (name != null && !name.isBlank()) {
            return name;
        }

        return memberId;
    }

    public String getAvatarText() {
        String displayName = getDisplayName();

        if (displayName == null || displayName.isBlank()) {
            return "U";
        }

        return displayName.substring(0, 1).toUpperCase();
    }
}
