package com.siyan1234.itproject2nd.config.handler;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
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

        MemberDto memberDto = resolveMemberDto(authentication);

        // 정상적인 회원 정보 못 꺼냈으면 로그인 화면으로
        if (memberDto == null || memberDto.getNo() == null) {

            response.sendRedirect(
                    SecurityPaths.withContextPath(
                            request,
                            SecurityPaths.MEMBER_LOGIN
                    )
            );

            return;
        }

        // DB와 현재 세션 안의 MemberDto에서 최근 로그인 시각을 갱신
        updateLastLoginDate(memberDto);

        // 이용약관 또는 개인정보 동의 값이 Y가 아니면 약관 동의 화면 주소를 선택
        if (isAgreementRequired(memberDto)) {

            // 신규 소셜 회원을 약관 동의 화면으로 이동시킴
            response.sendRedirect(
                    SecurityPaths.withContextPath(
                            request,
                            SecurityPaths.MEMBER_TERMS_AGREE
                    )
            );

            // 약관 미동의 회원을 메인 화면으로 보내지 않음.
            return;
        }

        // 이미 약관 동의 완료된 기존 소셜 회원은 메인 화면으로 이동.
        response.sendRedirect(
                SecurityPaths.withContextPath(
                        request,
                        SecurityPaths.HOME
                )
        );
    }

    private MemberDto resolveMemberDto(Authentication authentication) {

        // 인증 정보가 없거나 Principal 타입이 예상과 다르면 null 반환.
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {

            return null;
        }

        return customUserDetails.getMemberDto();
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
