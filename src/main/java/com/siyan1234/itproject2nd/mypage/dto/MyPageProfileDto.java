package com.siyan1234.itproject2nd.mypage.dto;

import com.siyan1234.itproject2nd.mypage.support.MyPageSocialProvider;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지에서 보여주는 회원 프로필 응답 DTO입니다.
 * member 테이블의 기본 정보와 social_account 연결 정보를 함께 담습니다.
 * boolean 형태의 getter는 Jackson이 JSON 응답 필드로 내려주므로 JS에서 바로 사용할 수 있습니다.
 */
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

    /**
     * 연결된 소셜 제공자 목록입니다.
     * mapper에서 LISTAGG 결과로 내려오며 예시는 "KAKAO, NAVER" 입니다.
     */
    private String socialProviders;

    /** 마이페이지 헤더와 메인 로그인 위젯에 표시할 이름입니다. */
    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }

        if (name != null && !name.isBlank()) {
            return name;
        }

        return memberId;
    }

    /** 프로필 이미지가 없을 때 사용할 한 글자 아바타입니다. */
    public String getAvatarText() {
        String displayName = getDisplayName();

        if (displayName == null || displayName.isBlank()) {
            return "U";
        }

        return displayName.substring(0, 1).toUpperCase();
    }

    /** 소셜 계정 연결 여부입니다. */
    public boolean isSocialLoginUser() {
        return "Y".equalsIgnoreCase(socialLoginYn);
    }

    /** JS에서 배열 형태로도 사용할 수 있도록 provider 목록을 제공합니다. */
    public List<String> getSocialProviderList() {
        return MyPageSocialProvider.normalizeProviders(socialProviders);
    }

    /** 화면에 표시할 로그인 방식 문구입니다. */
    public String getLoginMethodLabel() {
        if (!isSocialLoginUser()) {
            return "일반 로그인";
        }

        return MyPageSocialProvider.toLoginMethodLabel(socialProviders);
    }

    public boolean isKakaoConnected() {
        return MyPageSocialProvider.hasProvider(socialProviders, MyPageSocialProvider.KAKAO);
    }

    public boolean isNaverConnected() {
        return MyPageSocialProvider.hasProvider(socialProviders, MyPageSocialProvider.NAVER);
    }
}
