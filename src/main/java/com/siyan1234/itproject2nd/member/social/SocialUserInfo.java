package com.siyan1234.itproject2nd.member.social;

public interface SocialUserInfo {

    String getName(); // 실명 (카카오는 없을 수 있음)
    String getNickname(); // member.nickname NOT NULL 채울 값
    String getEmail(); // 이메일 (사용자가 동의 거부하면 null 가능)
    String getProvider(); // "kakao" 또는 "naver"
    String getProviderId(); // 그 provider 안에서의 고유 회원번호 (예: "3948573")
}
