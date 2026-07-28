package com.siyan1234.itproject2nd.mypage.support;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Spring Security Principal 형식이 달라도 마이페이지가 동일한 회원 번호를 얻도록 보정합니다.
 * CustomUserDetails를 우선 사용하고, 필요한 경우 Authentication과 회원 아이디 조회로 보완합니다.
 */
@Component
@RequiredArgsConstructor
public class MyPageLoginUserResolver {

    private static final String ANONYMOUS_USER = "anonymousUser";

    private final MemberDao memberDao;

    /**
     * 일반 로그인과 소셜 로그인 모두에서 현재 로그인 회원번호를 안정적으로 조회합니다.
     *
     * 우선순위:
     * 1. @AuthenticationPrincipal CustomUserDetails
     * 2. Authentication principal이 CustomUserDetails인 경우
     * 3. Authentication name(memberId)으로 DB 재조회
     */
    public Integer resolveMemberNo(CustomUserDetails customUserDetails, Authentication authentication) {
        Integer memberNo = resolveFromCustomUserDetails(customUserDetails);
        if (memberNo != null) {
            return memberNo;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (isAnonymousPrincipal(principal)) {
            return null;
        }

        if (principal instanceof CustomUserDetails customPrincipal) {
            return resolveFromCustomUserDetails(customPrincipal);
        }

        return resolveFromAuthenticationName(authentication.getName());
    }

    private Integer resolveFromCustomUserDetails(CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return null;
        }

        MemberDto memberDto = customUserDetails.getMemberDto();
        return memberDto == null ? null : memberDto.getNo();
    }

    private Integer resolveFromAuthenticationName(String memberId) {
        if (memberId == null || memberId.isBlank() || ANONYMOUS_USER.equals(memberId)) {
            return null;
        }

        MemberDto memberDto = memberDao.findByMemberId(memberId);
        return memberDto == null ? null : memberDto.getNo();
    }

    private boolean isAnonymousPrincipal(Object principal) {
        return principal instanceof String principalText && ANONYMOUS_USER.equals(principalText);
    }
}
