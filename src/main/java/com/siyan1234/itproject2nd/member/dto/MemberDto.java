package com.siyan1234.itproject2nd.member.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class MemberDto {

    private Long no;
    private String memberId;
    private String password; // BCrypt
    private String name;
    private String nickname; // NOT NULL
    private String email;
    private String phone;
    // 생년월일은 DATE -> 날짜만 담게 LocalDate
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;
    private String gender;
    private String postcode;
    private String address;
    private String detailAddress;
    private String role;
    private String profileImage; // 프로필 이미지 경로
    private LocalDateTime lastLoginDate; // 마지막 로그인 시각(시각까지 담음)
    private String agreeTermsYn; // 이용약관 동의 여부('Y'/'N')
    private String agreePrivacyYn; // 개인정보 동의 여부('Y'/'N')
    private LocalDateTime regdate; // 가입 시각(DB 기본값 SYSTIMESTAMP)
}
