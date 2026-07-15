package com.siyan1234.itproject2nd.member.social;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;

@RequiredArgsConstructor
public class KakaoUserInfo implements SocialUserInfo {

    // JSON을 Map으로 변환.
    private final Map<String, Object> attributes;

    @Override
    public String getName() {
        // 카카오는 실명 기본 제공 X, 실명 대신 닉네임. / 관리자 화면에 이름 빈칸 X, 닉네임으로 채우기.
        return getNickname();
    }

    @Override
    public String getNickname() {

        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");

        if (properties == null) {
            return null; // null을 반환하면 OAuth2DetailsService가 대체 닉네임 생성함.
        }

        Object nickname = properties.get("nickname"); // 상자 안에서 "nickname" 이름표 값 꺼냄

        if (nickname == null) {
            return null;
        }
        // toString() \ 어떤 타입이든 문자열로 바꾸는 메서드.
        return nickname.toString();
    }

    @Override
    public String getEmail() {
        // 이메일 "kakao_account"에 있음.
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null) {
            return null; // 이메일 동의 거부했거나 항목이 꺼져 있음.
        }

        // getOrDefault(찾을 이름표, 없을 때 대신 줄 값)
        // "email"이 있으면 그 값, 없으면 null
        return (String) kakaoAccount.getOrDefault("email", null);
    }

    @Override
    public String getProvider() {
        return "kakao"; // secret.yaml의 registration 이름과 반드시 일치해야 함.
    }

    @Override
    public String getProviderId() {
        return attributes.get("id").toString(); // id는 숫자(Long)로 오므로 toString()으로 문자열화.
    }
}
