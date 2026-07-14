package com.siyan1234.itproject2nd.cookie.interceptor;

import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.cookie.service.CookieConsentService;
import com.siyan1234.itproject2nd.cookie.service.VisitLogService;
import com.siyan1234.itproject2nd.cookie.support.VisitLogRequestFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 방문 기록 추적 Interceptor입니다.
 *
 * SECONDPRO_ANALYTICS=Y 쿠키가 있을 때만 visit_log에 기록합니다.
 * 비밀번호, 토큰, 요청 body, 쿠키 원문은 저장하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class VisitLogInterceptor implements HandlerInterceptor {

    private final CookieConsentService cookieConsentService;
    private final VisitLogRequestFactory visitLogRequestFactory;
    private final VisitLogService visitLogService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (shouldSkip(request)) {
            return true;
        }

        visitLogService.saveVisitLog(visitLogRequestFactory.create(request));
        return true;
    }

    private boolean shouldSkip(HttpServletRequest request) {
        return !cookieConsentService.isAnalyticsAllowed(request)
                || !SecurityPaths.isVisitLogTarget(request);
    }
}
