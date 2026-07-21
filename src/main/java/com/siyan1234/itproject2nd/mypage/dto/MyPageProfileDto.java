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

    /**
     * social_account 연결 여부입니다.
     * Oracle/MyBatis 매핑 안정성을 위해 Y/N 문자열로 받고, JSON 응답용 boolean getter를 별도로 제공합니다.
     */
    private String socialLoginYn;
    private String socialProviders;

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

    public boolean isSocialLoginUser() {
        return "Y".equalsIgnoreCase(socialLoginYn);
    }

    public String getLoginMethodLabel() {
        if (!isSocialLoginUser()) {
            return "일반 로그인";
        }

        if (socialProviders == null || socialProviders.isBlank()) {
            return "소셜 로그인";
        }

        return socialProviders + " 소셜 로그인";
    }

    public boolean isKakaoConnected() {
        return hasProvider("KAKAO");
    }

    public boolean isNaverConnected() {
        return hasProvider("NAVER");
    }

    private boolean hasProvider(String provider) {
        return socialProviders != null
                && socialProviders.toUpperCase().contains(provider);
    }
}
