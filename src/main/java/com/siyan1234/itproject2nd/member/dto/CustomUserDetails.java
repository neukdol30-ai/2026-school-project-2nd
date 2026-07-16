package com.siyan1234.itproject2nd.member.dto;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

// UserDetails(일반 폼 로그인용 규격) / OAuth2User(소셜 로그인용 규격)
// 둘을 하나로 합쳐야 @AuthenticationPrincipal CustomUserDetails로 일반, 소셜 로그인 똑같이 꺼내 쓸 수 있음.
public class CustomUserDetails implements UserDetails, OAuth2User {

    private final MemberDto memberDto; // 로그인한 회원 정보

    private Map<String, Object> oauth2Attributes; // 소셜 원본 JSON, 일반 폼 로그인 때는 null

    // 생성자 1 - 일반 폼 로그인용. CustomUserDetailService가 이걸 호출
    public CustomUserDetails(MemberDto memberDto) {
        this.memberDto = memberDto; // oauth2Attributes는 null 상태
    }

    // 생성자 2 - 소셜 로그인용. OAuth2DetailsService가 이걸 호출
    public CustomUserDetails(MemberDto memberDto, Map<String, Object> oauth2Attributes) {
        this.memberDto = memberDto;
        this.oauth2Attributes = oauth2Attributes;
    }

    // 컨트롤러 등에서 로그인한 회원 정보 꺼내 쓰기 위한 통로
    public MemberDto getMemberDto() {
        return memberDto;
    }

    // OAuth2User 규격 요구 메서드
    @Override
    public Map<String, Object> getAttributes() {
        return oauth2Attributes == null ? Map.of() : oauth2Attributes;
    }

    @Override
    public String getName() {
        return memberDto.getMemberId();
    }

    // UserDetails 규격 요구 메서드
    // 권한 목록: 이 회원이 가진 권한 Security에 전달
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // DB의 role은 'USER'/'ADMIN' 문자열. -> Security hasRole("ADMIN")은 "ROLE_ADMIN"을 찾음. -> "ROLE_" 붙이기.
        return List.of(new SimpleGrantedAuthority("ROLE_" + memberDto.getRole()));
    }

    @Override
    public String getPassword() {
        // 소셜 회원도 무작위 BCrypt 해시 들어 있음. -> null 아님.
        return memberDto.getPassword();
    }

    @Override // 아이디: Security가 말하는 "username"은 우리 기준 memberId(로그인 아이디)
    public String getUsername() {
        return memberDto.getMemberId();
    }

    // 여기 4개는 계정 상태 점검용. 현재는 정상(true)으로 둠.
    // 나중에 탈퇴/정지/기간만료 기능 넣을 때 false 조건 여기서 처리
    @Override
    public boolean isAccountNonExpired() {
        return true; // 계정 만료 안 됨
    }

    @Override
    public boolean isAccountNonLocked() {
        return memberDto == null || !memberDto.isBanned(); //계정 잠김 안 됨
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비번 만료 안 됨
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 사용 가능. 관리자 정지는 isAccountNonLocked에서 처리.
    }
}
