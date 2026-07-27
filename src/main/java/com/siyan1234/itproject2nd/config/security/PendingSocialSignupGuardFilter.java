package com.siyan1234.itproject2nd.config.security;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// 우리 서비스 약관에 아직 동의하지 않은 사용자가 다른 서비스 화면으로 새어나가지 못하게 막는 Security 필터
@Component
public class PendingSocialSignupGuardFilter extends OncePerRequestFilter {

    // 한 HTTP 요청마다 한 번 실행되는 필터 메서드
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 로그인 정보 자체가 없으면 이 필터가 상관할 요청이 아님
        if (authentication == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 우리 프로젝트 로그인 객체가 아니면 통과. 비로그인 익명 사용자가 여기서 걸러짐
        if (!(authentication.getPrincipal() instanceof CustomUserDetails loginUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 약관 동의가 끝난 정상 사용자면 통과
        if (!isAgreementPending(loginUser)) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestPath = normalizePath(request);

        // 약관 절차에 필요한 주소면 통과
        if (isAllowedPendingPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 그 외 주소는 Controller 실행 없이 약관 동의 화면으로 돌려보냄
        response.sendRedirect(SecurityPaths.withContextPath(request, SecurityPaths.MEMBER_TERMS_AGREE));
    }

    // 우리 서비스 약관 동의를 아직 끝내지 않은 사용자인지 판단
    private boolean isAgreementPending(CustomUserDetails loginUser) {
        // 경우 1. STEP 6-B 방식으로 들어온 신규 소셜 사용자. DB에 회원 행이 없고 Redis에만 임시 저장된 상태
        if (loginUser.isPendingSocialSignup()) {
            return true;
        }

        // 경우 2. STEP 6-B 적용 전에 이미 DB에 N/N으로 저장된 옛 회원
        MemberDto memberDto = loginUser.getMemberDto();

        // 회원 정보가 없으면 판단 근거가 없으므로 막지 않음. 여기서 true면 예외 상황에 사용자가 갇힘
        if (memberDto == null) {
            return false;
        }

        boolean termsAgreementRequired = !"Y".equalsIgnoreCase(memberDto.getAgreeTermsYn());
        boolean privacyAgreementRequired = !"Y".equalsIgnoreCase(memberDto.getAgreePrivacyYn());

        return termsAgreementRequired || privacyAgreementRequired;
    }

    // 약관 동의 전에도 접근을 허용할 주소인지 확인
    private boolean isAllowedPendingPath(String requestPath) {
        // 주소를 계산하지 못한 비정상 상황이면 허용하지 않음
        if (requestPath == null) {
            return false;
        }

        // 약관 동의 화면(GET)과 동의 처리 요청(POST). 막으면 무한 리다이렉트가 됨
        if (SecurityPaths.MEMBER_TERMS_AGREE.equals(requestPath)) {
            return true;
        }

        // 동의하지 않고 가입을 취소하는 주소
        if (SecurityPaths.MEMBER_TERMS_AGREE_CANCEL.equals(requestPath)) {
            return true;
        }

        // 로그아웃 주소. 막으면 동의하기 싫은 사용자가 로그아웃조차 못 하고 갇힘
        // SecurityPaths에 상수가 없고 SecurityConfig도 문자열을 쓰므로 문자열로 맞춤
        if ("/member/logout".equals(requestPath)) {
            return true;
        }

        // 이용약관 전문, 개인정보 전문. 동의 전에 읽어야 하므로 허용
        if (SecurityPaths.MEMBER_TERMS.equals(requestPath) || SecurityPaths.MEMBER_PRIVACY.equals(requestPath)) {
            return true;
        }

        // 화면을 그리는 데 필요한 정적 파일과 오류 처리 경로
        return requestPath.startsWith("/css/")
                || requestPath.startsWith("/js/")
                || requestPath.startsWith("/images/")
                || requestPath.startsWith("/favicon")
                || requestPath.startsWith("/error");
    }

    // 요청 주소에서 프로젝트 앞머리 경로(context path)를 잘라냄
    private String normalizePath(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null) {
            return null;
        }

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            return requestUri.substring(contextPath.length());
        }

        return requestUri;
    }
}