package com.siyan1234.itproject2nd.member.social;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;

public class NaverUserInfo implements SocialUserInfo {

    private final Map<String, Object> attributes; // 네이버가 준 원본 전체
    private final Map<String, Object> response; // 실제 알맹이가 든 상자

    public NaverUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getName() {
        if (response == null) return null;
        Object name = response.get("name");
        return name == null ? null : name.toString(); // 삼항 연산자: 조건 ? 참일때 값 : 거짓일 때 값
    }

    @Override
    public String getNickname() {
        if (response == null) return null;
        Object nickname = response.get("nickname"); // '별명' 동의 항목이 꺼지면 안 온다.
        return nickname == null ? null : nickname.toString();
    }

    @Override
    public String getEmail() {
        if (response == null) return null;
        return (String) response.get("email");
    }

    @Override
    public String getProvider() {
        return "naver";
    }

    @Override
    public String getProviderId() {
        if (response == null) return null;
        // 네이버의 id는 카카오와 달리 처음부터 문자열이지만, 타입이 Object라 toString()으로 통일
        return response.get("id").toString();
    }
}
