package com.siyan1234.itproject2nd.member.dto;

import com.siyan1234.itproject2nd.config.security.SecurityAuthority;
import org.jspecify.annotations.Nullable;
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

    // 현재 로그인 사용자에게 연결된 회원 정보
    private final MemberDto memberDto;

    private final Map<String, Object> oauth2Attributes;

    private final boolean pendingSocialSignup;

    private final String pendingSocialToken;

    public CustomUserDetails(MemberDto memberDto) {

        this(memberDto, Map.of(), false, null);
    }

    public CustomUserDetails(MemberDto memberDto,
                             Map<String, Object> oauth2Attributes) {
        this(memberDto, oauth2Attributes, false, null);
    }

    public CustomUserDetails(MemberDto memberDto,
                             Map<String, Object> oauth2Attributes,
                             String pendingSocialToken) {
        this(memberDto, oauth2Attributes, true, pendingSocialToken);
    }

    private CustomUserDetails(MemberDto memberDto,
                              Map<String, Object> oauth2Attributes,
                              boolean pendingSocialSignup,
                              String pendingSocialToken) {
        this.memberDto = memberDto;

        this.oauth2Attributes = oauth2Attributes == null ? Map.of() : oauth2Attributes;

        this.pendingSocialSignup = pendingSocialSignup;

        this.pendingSocialToken = pendingSocialToken;
    }

    public MemberDto getMemberDto() {

        return memberDto;
    }

    public boolean isPendingSocialSignup() {

        return pendingSocialSignup;
    }

    public String getPendingSocialToken() {

        return pendingSocialToken;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2Attributes;
    }

    @Override
    public String getName() {
        return getUsername();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {

        if (pendingSocialSignup) {

            return List.of(new SimpleGrantedAuthority(
                    SecurityAuthority.ROLE_PENDING_SOCIAL));
        }

        return List.of(new SimpleGrantedAuthority(
                "ROLE_" + memberDto.getRole()));
    }

    @Override
    public String getPassword() {

        return memberDto.getPassword();
    }

    @Override
    public String getUsername() {

        return memberDto.getMemberId();
    }

    @Override
    public boolean isAccountNonExpired() {

        return true;
    }

    @Override
    public boolean isAccountNonLocked() {

        return memberDto == null || !memberDto.isBanned();
    }

    @Override
    public boolean isCredentialsNonExpired() {

        return true;
    }

    @Override
    public boolean isEnabled() {

        return true;
    }
}
