package com.siyan1234.itproject2nd.cookie.interceptor;

import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import com.siyan1234.itproject2nd.cookie.service.VisitLogService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;

/**
 * 방문 기록 추적 Interceptor입니다.
 *
 * SECONDPRO_ANALYTICS=Y 쿠키가 있을 때만 visit_log에 기록합니다.
 * 비밀번호, 토큰, 요청 body, 쿠키 원문은 저장하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class VisitLogInterceptor implements HandlerInterceptor {

    private static final String ANALYTICS_COOKIE_NAME = "SECONDPRO_ANALYTICS";

    private final VisitLogService visitLogService;
    private final LoginMemberResolver loginMemberResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!isAnalyticsAllowed(request) || !isTrackableRequest(request)) {
            return true;
        }

        VisitLogDto visitLogDto = new VisitLogDto();
        visitLogDto.setMemberNo(loginMemberResolver.getCurrentMemberNo());
        visitLogDto.setSessionId(limit(getSessionId(request), 100));
        visitLogDto.setRequestUri(limit(request.getRequestURI(), 500));
        visitLogDto.setQueryString(limit(request.getQueryString(), 1000));
        visitLogDto.setRequestMethod(limit(request.getMethod(), 20));
        visitLogDto.setUserAgent(limit(request.getHeader("User-Agent"), 1000));
        visitLogDto.setIpAddress(limit(getClientIp(request), 100));
        visitLogDto.setReferer(limit(request.getHeader("Referer"), 1000));
        visitLogDto.setCreatedDate(LocalDateTime.now());

        visitLogService.saveVisitLog(visitLogDto);
        return true;
    }

    private boolean isAnalyticsAllowed(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (ANALYTICS_COOKIE_NAME.equals(cookie.getName()) && "Y".equalsIgnoreCase(cookie.getValue())) {
                return true;
            }
        }

        return false;
    }

    private boolean isTrackableRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();

        if (uri == null) {
            return false;
        }

        return !uri.startsWith("/css/")
                && !uri.startsWith("/js/")
                && !uri.startsWith("/images/")
                && !uri.startsWith("/favicon")
                && !uri.startsWith("/ws/")
                && !uri.startsWith("/error");
    }

    private String getSessionId(HttpServletRequest request) {
        if (request.getSession(false) == null) {
            return null;
        }

        return request.getSession(false).getId();
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");

        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }

        return request.getRemoteAddr();
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return null;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
