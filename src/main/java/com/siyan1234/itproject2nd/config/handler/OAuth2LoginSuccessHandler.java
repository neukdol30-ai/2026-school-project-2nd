package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

// 카카오 / 네이버 소셜 로그인 성공 후 실행되는 전용 Handler
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    // 최근 로그인 시각을 DB에 저장하기 위한 DAO
    private final MemberDao memberDao;

    // 소셜 로그인 성공 시 Spring Security가 이 메서드 호출
    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, // 카카오 / 네이버 로그인 성공 요청
            HttpServletResponse response, // 브라우저에 리다이렉트 응답 보낼 객체
            Authentication authentication // 로그인 성공한 회원 인증 정보
    ) throws IOException, ServletException {
        CustomUserDetails loginUser = resolveLoginUser(authentication);

        // 우리 로그인 객체를 꺼내지 못했으면 로그인 화면으로
        if (loginUser == null) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_LOGIN));
            return;
        }

        // 약관 동의 대기 상태(DB에 아직 회원 행이 없음) 곧바로 약관 동의 화면으로
        if (loginUser.isPendingSocialSignup()) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_TERMS_AGREE));
            return;
        }

        MemberDto memberDto = loginUser.getMemberDto();

        if (memberDto == null || memberDto.getNo() == null) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_LOGIN));
            return;
        }

        updateLastLoginDate(memberDto);

        if (isAgreementRequired(memberDto)) {
            response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_TERMS_AGREE));
            return;
        }

        // 이미 약관 동의 완료된 기존 소셜 회원은 메인 화면으로 이동.
        response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.HOME));
    }

    // 인증 정보에서 우리 프로젝트 로그인 객체를 꺼냄
    private CustomUserDetails resolveLoginUser(Authentication authentication) {
        // 인증 정보가 없거나 Principal 타입이 예상과 다르면 null 반환.
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            return null;
        }

        return customUserDetails;
    }

    private void updateLastLoginDate(MemberDto memberDto) {
        // 회원 정보나 회원번호가 없다면 DB UPDATE 실행 X.
        if (memberDto == null || memberDto.getNo() == null) {
            return;
        }

        // MemberMapper.xml의 updateLastLoginDate 실행해 DB 시각으로 갱신.
        memberDao.updateLastLoginDate(memberDto.getNo());
        memberDto.setLastLoginDate(LocalDateTime.now());
    }

    // 서비스 약관 동의 화면이 필요한 회원인지 판단
    private boolean isAgreementRequired(MemberDto memberDto) {
        boolean termsAgreementRequired = !"Y".equalsIgnoreCase(memberDto.getAgreeTermsYn());
        boolean privacyAgreementRequired = !"Y".equalsIgnoreCase(memberDto.getAgreePrivacyYn());

        return termsAgreementRequired || privacyAgreementRequired;
    }
}
