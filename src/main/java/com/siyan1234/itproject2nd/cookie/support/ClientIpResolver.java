package com.siyan1234.itproject2nd.cookie.support;

import com.siyan1234.itproject2nd.cookie.config.VisitLogProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 방문 통계에 사용할 클라이언트 IP를 결정합니다.
 *
 * 기본값은 서블릿 컨테이너가 제공하는 remoteAddr입니다. 프록시 전달 헤더는
 * VisitLogProperties에서 명시적으로 허용한 경우에만 사용하여 헤더 위조를 방지합니다.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final Pattern NUMERIC_IP_CHARACTERS = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final VisitLogProperties visitLogProperties;

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String remoteAddress = normalizeNumericIp(request.getRemoteAddr());

        if (!visitLogProperties.isTrustForwardedHeaders()) {
            return remoteAddress;
        }

        String forwardedAddress = firstForwardedAddress(request.getHeader("X-Forwarded-For"));
        String normalizedForwardedAddress = normalizeNumericIp(forwardedAddress);

        if (normalizedForwardedAddress != null) {
            return normalizedForwardedAddress;
        }

        String realAddress = normalizeNumericIp(request.getHeader("X-Real-IP"));
        return realAddress == null ? remoteAddress : realAddress;
    }

    private String firstForwardedAddress(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return null;
        }

        return forwardedFor.split(",", 2)[0].trim();
    }

    private String normalizeNumericIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String candidate = value.trim();

        if (candidate.length() >= 2 && candidate.startsWith("\"") && candidate.endsWith("\"")) {
            candidate = candidate.substring(1, candidate.length() - 1).trim();
        }

        if (candidate.startsWith("[") && candidate.endsWith("]")) {
            candidate = candidate.substring(1, candidate.length() - 1);
        }

        int zoneIndex = candidate.indexOf('%');
        if (zoneIndex >= 0) {
            candidate = candidate.substring(0, zoneIndex);
        }

        if (candidate.isBlank()
                || "unknown".equals(candidate.toLowerCase(Locale.ROOT))
                || (!candidate.contains(".") && !candidate.contains(":"))
                || !NUMERIC_IP_CHARACTERS.matcher(candidate).matches()) {
            return null;
        }

        try {
            InetAddress address = InetAddress.getByName(candidate);

            // localhost 접속 방식이 달라도 같은 방문자로 집계되도록
            // IPv4/IPv6 루프백 주소를 127.0.0.1로 통일합니다.
            if (address.isLoopbackAddress()) {
                return "127.0.0.1";
            }

            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }
}
