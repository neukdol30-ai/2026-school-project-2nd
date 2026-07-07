package com.siyan1234.itproject2nd.member.dto;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    // 로그인한 회원 정보
    private final MemberDto memberDto;

    // 생성자: 조회한 회원 정보를 받아 보관
    public CustomUserDetails(MemberDto memberDto) {
        this.memberDto = memberDto;
    }

    // 컨트롤러 등에서 로그인한 회원 정보 꺼내 쓰기 위한 통로
    public MemberDto getMemberDto() {
        return memberDto;
    }

    // 권한 목록: 이 회원이 가진 권한 Security에 전달
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // DB의 role은 'USER'/'ADMIN' 문자열. -> Security hasRole("ADMIN")은 "ROLE_ADMIN"을 찾음. -> "ROLE_" 붙이기.
        return List.of(new SimpleGrantedAuthority("ROLE_" + memberDto.getRole()));
    }

    @Override
    public String getPassword() {
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
        return true; // 계정 잠김 안 됨
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // 비번 만료 안 됨
    }

    @Override
    public boolean isEnabled() {
        return true; // 계정 사용 가능
    }
    
}
