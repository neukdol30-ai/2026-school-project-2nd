package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

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
