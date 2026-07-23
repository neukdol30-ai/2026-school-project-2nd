package com.siyan1234.itproject2nd.calendar.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class GoogleCalendarTokenDto {

    // 토큰 번호
    private Integer no;

    // 우리 사이트 회원 번호
    private Integer memberNo;

    // 구글 API 호출용 토큰
    private String accessToken;

    // accessToken 재발급용 토큰
    private String refreshToken;

    // accessToken 만료 시간
    private LocalDateTime tokenExpiry;

    // 연동된 구글 이메일
    private String googleEmail;

    // 연동 등록일
    private LocalDateTime regdate;
}