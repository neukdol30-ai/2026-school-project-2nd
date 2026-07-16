package com.siyan1234.itproject2nd.cookie.support;

import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.cookie.dto.VisitLogDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** HttpServletRequest를 visit_log 저장용 DTO로 변환합니다. */
@Component
@RequiredArgsConstructor
public class VisitLogRequestFactory {

    private final LoginMemberResolver loginMemberResolver;

    public VisitLogDto create(HttpServletRequest request) {
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
        return visitLogDto;
    }

    private String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session == null ? null : session.getId();
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
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
