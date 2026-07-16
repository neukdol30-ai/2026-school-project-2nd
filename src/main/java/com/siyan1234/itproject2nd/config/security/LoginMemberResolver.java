package com.siyan1234.itproject2nd.config.security;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Spring Security 인증 객체에서 현재 로그인 회원 정보를 꺼내는 공통 컴포넌트입니다.
 *
 * 사용 위치:
 * - 관리자 Controller: 접속 관리자 프로필/권한 확인
 * - 채팅 Controller: 사용자 상담방 접근 권한 확인
 * - 방문 기록 Interceptor: 로그인 회원 번호 기록
 */
@Component
public class LoginMemberResolver {

    public MemberDto fromPrincipal(CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return null;
        }

        return customUserDetails.getMemberDto();
    }

    public MemberDto getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (isAnonymous(authentication)) {
            return null;
        }

        return resolvePrincipal(authentication.getPrincipal());
    }

    public Integer getCurrentMemberNo() {
        MemberDto memberDto = getCurrentMember();
        return memberDto == null ? null : memberDto.getNo();
    }

    public boolean isAdmin(MemberDto memberDto) {
        return memberDto != null && SecurityAuthority.ADMIN.equals(memberDto.getRole());
    }

    private boolean isAnonymous(Authentication authentication) {
        return authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken;
    }

    private MemberDto resolvePrincipal(Object principal) {
        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMemberDto();
        }

        if (principal instanceof MemberDto memberDto) {
            return memberDto;
        }

        return null;
    }
}
