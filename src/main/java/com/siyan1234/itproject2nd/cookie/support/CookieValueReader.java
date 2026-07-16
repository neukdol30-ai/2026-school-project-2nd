package com.siyan1234.itproject2nd.cookie.support;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/** HttpServletRequest에서 쿠키 값을 안전하게 읽는 공통 컴포넌트입니다. */
@Component
public class CookieValueReader {

    public String findValue(HttpServletRequest request, String cookieName) {
        if (request == null || cookieName == null || request.getCookies() == null) {
            return null;
        }

        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }

        return null;
    }

    public boolean hasValue(HttpServletRequest request, String cookieName, String expectedValue) {
        String actualValue = findValue(request, cookieName);
        return expectedValue != null && expectedValue.equalsIgnoreCase(actualValue);
    }
}
