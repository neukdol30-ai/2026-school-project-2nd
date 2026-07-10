package com.siyan1234.itproject2nd.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class SignupDto { // 회원가입 화면 값
    // @NotBlank : 빈 값이면 오류 / @Size : 글자 수 4~20자만 허용 (화면 minlength,maxlength와 같은 값으로 맞춤)
    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력하세요.")
    private String memberId;

    // @Pattern : 정규식과 일치하면 통과. 대,소문자/숫자 각 1개 이상 + 8~12자 (화면 pattern과 동일)
    // regexp 안의 \\d는 java 문자열이라 역슬래시 2개 사용(\d를 표현). HTML에선 \d 하나였음.
    @NotBlank(message = "비밀번호를 입력하세요.")
    @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,12}",
    message = "비밀번호는 영문 대문자·소문자·숫자를 모두 포함해 8~12자로 입력하세요.")
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordCheck;

    @NotBlank(message = "이름을 입력하세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력하세요")
    private String nickname;

    @NotBlank(message = "이메일을 입력하세요.")
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
