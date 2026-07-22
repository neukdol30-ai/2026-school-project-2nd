package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * 마이페이지 수정 요청 DTO입니다.
 * 내 정보 탭에서는 name/nickname만 사용하고,
 * 보안 설정 탭에서는 email/phone/birthDate/gender/address 계열을 사용합니다.
 */
@Getter
@Setter
public class MyPageUpdateDto {

    private String name;
    private String nickname;
    private String email;
    private String phone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String gender;
    private String postcode;
    private String address;
    private String detailAddress;
    private String profileImage;
}
