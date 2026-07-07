package com.siyan1234.itproject2nd.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class SignupDto { // 회원가입 화면에서 넘어오는 값을 담는 DTO

    @NotBlank(message = "아이디를 입력하세요.") // 빈 값이면 검증 오류 만듦
    private String memberId;

    @NotBlank(message = "비밀번호를 입력하세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordCheck;

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력하세요")
    private String nickname;

    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd") // HTML date 입력값을 LocalDate로 변경
    private LocalDate birthDate;

    private String gender;

    private String postcode;

    private String address;

    private String detailAddress;

    private String agreeTermsYn;

    private String agreePrivacyYn;
}
