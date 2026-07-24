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

    private final MemberDao memberDao;

    // 소셜 로그인 성공 시 Spring Security가 이 메서드 호출

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, // 카카오 / 네이버 로그인 성공 요청
            HttpServletResponse response, // 브라우저에 리다이렉트 응답 보낼 객체
            Authentication authentication // 로그인 성공한 회원 인증 정보
    ) throws IOException, ServletException {

        updateLastLoginDate(authentication); // DB와 현재 로그인 객체의 최근 로그인 시각 갱신

        response.sendRedirect(
                SecurityPaths.withContextPath(
                        request, // 현 프로젝트의 Context Path 정보 확인
                        SecurityPaths.HOME));
    }

    // 로그인한 회원 번호 꺼내 member.last_login_date를 현재 시각으로 수정
    private void updateLastLoginDate(Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            return; // NullPointerException을 막기 위한 방어 처리
        }

        MemberDto memberDto = customUserDetails.getMemberDto();

        if (memberDto == null || memberDto.getNo() == null) {
            return; // 잘못된 인증 객체로 인한 오류 방어
        }

        // MemberMapper.xml - updateLastLoginDate 실행 -> DB의 last_login_date를 현재 DB 시각으로 갱신
        memberDao.updateLastLoginDate(memberDto.getNo());

        // 현재 세션 안의 MemberDto도 현재 시각으로 수정.
        memberDto.setLastLoginDate(LocalDateTime.now());
    }
}
