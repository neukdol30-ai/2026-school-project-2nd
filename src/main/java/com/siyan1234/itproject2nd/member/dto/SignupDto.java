package com.siyan1234.itproject2nd.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import com.siyan1234.itproject2nd.config.security.PasswordPolicy;

import java.time.LocalDate;

@Getter
@Setter
public class SignupDto { // 회원가입 화면 값

    // @NotBlank : 빈 값이면 오류 / @Size : 글자 수 4~20자만 허용 (화면 minlength,maxlength와 같은 값으로 맞춤)
    @NotBlank(message = "아이디를 입력하세요.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력하세요.")
    private String memberId;

    // @Pattern : 정규식과 일치하면 통과. 대문자·소문자·숫자 각 1개 이상 + 8~20자
    @NotBlank(message = "비밀번호를 입력하세요.")
    @Pattern(regexp = PasswordPolicy.PASSWORD_REGEX,
            message = PasswordPolicy.PASSWORD_MESSAGE)
    private String password;

    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String passwordCheck;

    @NotBlank(message = "이름을 입력하세요.")
    @Size(min = 2, max = 20, message = "이름은 2자 이상 20자 이하로 입력하세요.")
    private String name;

    @NotBlank(message = "닉네임을 입력하세요")
    @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력하세요.")
    private String nickname;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;

    // 전화번호는 "선택" 항목이므로 안 적어도 통과해야 함.
    // regexp 해설 :
    // ^$ -> 빈 문자열이면 통과 (전화번호를 안 적은 경우)
    // | -> 또는(OR)
    // ^010-\\d{4}-\\d{4}$ -> 010- 로 시작 + 숫자4자리 + - + 숫자4자리 로 끝나면 통과
    // \\d{4} : 숫자(\d) 정확히 4개({4})
    // ※ @Pattern은 값이 null이면 검사 자체를 건너뛴다(=통과). 하지만 HTML 폼은 빈 칸을 null이 아니라
    // 빈 문자열("")로 보내므로, ^$ 를 반드시 넣어야 "전화번호 미입력"이 오류로 안 걸린다.
    @Pattern(regexp = "^$|^010-\\d{4}-\\d{4}$",
            message = "전화번호는 010-0000-0000 형식으로 입력하세요.")
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
