package com.siyan1234.itproject2nd.cookie.service;

import com.siyan1234.itproject2nd.cookie.support.CookieNames;
import com.siyan1234.itproject2nd.cookie.support.CookieValueReader;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 쿠키 동의 여부 판단을 담당합니다. */
@Service
@RequiredArgsConstructor
public class CookieConsentService {

    private static final String ALLOWED_VALUE = "Y";

    private final CookieValueReader cookieValueReader;

    public boolean isAnalyticsAllowed(HttpServletRequest request) {
        return cookieValueReader.hasValue(request, CookieNames.ANALYTICS, ALLOWED_VALUE);
    }

    public boolean isPersonalizationAllowed(HttpServletRequest request) {
        return cookieValueReader.hasValue(request, CookieNames.PERSONALIZATION, ALLOWED_VALUE);
    }
}
