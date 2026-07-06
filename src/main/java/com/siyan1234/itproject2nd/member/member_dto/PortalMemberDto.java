package com.siyan1234.itproject2nd.member.member_dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(exclude = {"password", "passwordCheck"})
public class PortalMemberDto {

    // 회원 번호
    // DB에서 IDENTITY로 자동 증가하므로 INSERT 때 직접 넣지 않음
    private Long no;

    // 로그인 아이디
    // MEMBER.MEMBER_ID
    private String memberId;

    // 비밀번호
    // MEMBER.PASSWORD
    private String password;

    // 비밀번호 확인
    // DB 컬럼 아님
    // 회원가입 검사용
    private String passwordCheck;

    // 이름
    // MEMBER.NAME
    private String name;

    // 닉네임
    // DB에서 NOT NULL이라 회원가입 때 반드시 받아야 함
    private String nickname;

    // 이메일
    // MEMBER.EMAIL
    private String email;

    // 전화번호
    // MEMBER.PHONE
    private String phone;

    // 생년월일
    // MEMBER.BIRTH_DATE
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    // 성별
    // MEMBER.GENDER
    private String gender;

    // 우편번호
    // MEMBER.POSTCODE
    private String postcode;

    // 주소
    // MEMBER.ADDRESS
    private String address;

    // 상세주소
    // MEMBER.DETAIL_ADDRESS
    private String detailAddress;

    // 권한
    // MEMBER.ROLE
    private String role;

    // 프로필 이미지
    // MEMBER.PROFILE_IMAGE
    private String profileImage;

    // 마지막 로그인 날짜
    // MEMBER.LAST_LOGIN_DATE
    private LocalDateTime lastLoginDate;

    // 이용약관 동의 여부
    // 체크하면 Y, 안 하면 null로 들어옴
    private String agreeTermsYn;

    // 개인정보 동의 여부
    // 체크하면 Y, 안 하면 null로 들어옴
    private String agreePrivacyYn;

    // 가입일
    // DB 기본값 SYSTIMESTAMP
    private LocalDateTime regdate;
}