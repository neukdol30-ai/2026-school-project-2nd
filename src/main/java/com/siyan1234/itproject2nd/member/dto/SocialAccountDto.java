package com.siyan1234.itproject2nd.member.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccountDto {

    private Integer no; // PK. DB가 자동 부여 (IDENTITY)
    private Integer memberNo;
    private String provider; // "kakao" / "naver"
    private String providerId; // provider의 고유 회원 번호.
    private LocalDateTime createdDate; // DB DEFAULT SYSTIMESTAMP
}
