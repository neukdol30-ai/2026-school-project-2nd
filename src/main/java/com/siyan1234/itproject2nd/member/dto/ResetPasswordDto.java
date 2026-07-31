package com.siyan1234.itproject2nd.member.dto;

import com.siyan1234.itproject2nd.config.security.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordDto { // 비밀번호 재설정 화면에서 넘어오는 값

    @NotBlank(message = "새 비밀번호를 입력하세요.")
    @Pattern(regexp = PasswordPolicy.PASSWORD_REGEX, message = PasswordPolicy.PASSWORD_MESSAGE)
    private String newPassword;

    // 비밀번호 확인란은 형식 검사가 필요 없음. / 일치 여부는 Controller에서 직접 비교함
    @NotBlank(message = "비밀번호 확인을 입력하세요.")
    private String newPasswordCheck;
}
