package com.siyan1234.itproject2nd.member.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 소셜 인증 성공 -> 서비스 약관 아직 동의 X(가입 대기 사용자 최소 정보 DTO)
// 이 객체의 값은 member 테이블에 바로 저장 X -> Redis에 짧은 시간 동안만 임시 저장
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingSocialSignupDto {

    private String provider;

    private String providerId;

    private String name;

    private String nickname;

    private String email;
}
