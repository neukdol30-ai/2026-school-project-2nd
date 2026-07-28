package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dto.ServiceStatusDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 관리자 화면에 표시할 외부 서비스 설정 상태를 조립합니다.
 *
 * <p>관리자 화면 진입 때 실제 외부 API를 호출하면 응답 지연과 호출량 증가가 발생할 수 있으므로,
 * 비밀 값 자체는 노출하지 않고 현재 서버에 필수 키·토큰이 등록되어 있는지만 확인합니다.</p>
 */
@Service
public class ExternalServiceStatusService {

    private final String naverNewsClientId;
    private final String naverNewsClientSecret;
    private final String dataGoServiceKey;
    private final String kakaoMapRestApiKey;
    private final String kakaoMapJavascriptKey;
    private final boolean kakaoNotifyEnabled;
    private final String kakaoNotifyRestApiKey;
    private final String kakaoRefreshToken;
    private final String googleCalendarClientId;
    private final String googleCalendarClientSecret;
    private final String googleCalendarRedirectUri;
    private final String googleCalendarScope;
    private final String googleHolidayApiKey;
    private final String mailUsername;
    private final String mailPassword;
    private final String kakaoLoginClientId;
    private final String kakaoLoginClientSecret;
    private final String naverLoginClientId;
    private final String naverLoginClientSecret;

    public ExternalServiceStatusService(
            @Value("${naver.client-id:}") String naverNewsClientId,
            @Value("${naver.client-secret:}") String naverNewsClientSecret,
            @Value("${data-go.service-key:}") String dataGoServiceKey,
            @Value("${kakao.map.rest-api-key:}") String kakaoMapRestApiKey,
            @Value("${kakao.map.javascript-key:}") String kakaoMapJavascriptKey,
            @Value("${kakao.notify.enabled:false}") boolean kakaoNotifyEnabled,
            @Value("${kakao.notify.rest-api-key:}") String kakaoNotifyRestApiKey,
            @Value("${kakao.notify.admin-refresh-token:}") String kakaoRefreshToken,
            @Value("${google.calendar.client-id:}") String googleCalendarClientId,
            @Value("${google.calendar.client-secret:}") String googleCalendarClientSecret,
            @Value("${google.calendar.redirect-uri:}") String googleCalendarRedirectUri,
            @Value("${google.calendar.scope:}") String googleCalendarScope,
            @Value("${google.calendar.holiday-api-key:}") String googleHolidayApiKey,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${spring.security.oauth2.client.registration.kakao.client-id:}") String kakaoLoginClientId,
            @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}") String kakaoLoginClientSecret,
            @Value("${spring.security.oauth2.client.registration.naver.client-id:}") String naverLoginClientId,
            @Value("${spring.security.oauth2.client.registration.naver.client-secret:}") String naverLoginClientSecret
    ) {
        this.naverNewsClientId = naverNewsClientId;
        this.naverNewsClientSecret = naverNewsClientSecret;
        this.dataGoServiceKey = dataGoServiceKey;
        this.kakaoMapRestApiKey = kakaoMapRestApiKey;
        this.kakaoMapJavascriptKey = kakaoMapJavascriptKey;
        this.kakaoNotifyEnabled = kakaoNotifyEnabled;
        this.kakaoNotifyRestApiKey = kakaoNotifyRestApiKey;
        this.kakaoRefreshToken = kakaoRefreshToken;
        this.googleCalendarClientId = googleCalendarClientId;
        this.googleCalendarClientSecret = googleCalendarClientSecret;
        this.googleCalendarRedirectUri = googleCalendarRedirectUri;
        this.googleCalendarScope = googleCalendarScope;
        this.googleHolidayApiKey = googleHolidayApiKey;
        this.mailUsername = mailUsername;
        this.mailPassword = mailPassword;
        this.kakaoLoginClientId = kakaoLoginClientId;
        this.kakaoLoginClientSecret = kakaoLoginClientSecret;
        this.naverLoginClientId = naverLoginClientId;
        this.naverLoginClientSecret = naverLoginClientSecret;
    }

    public List<ServiceStatusDto> getServiceStatuses() {
        boolean naverNewsAvailable = hasAllText(naverNewsClientId, naverNewsClientSecret);
        boolean dataGoAvailable = hasText(dataGoServiceKey);
        boolean kakaoMapRestAvailable = hasText(kakaoMapRestApiKey);
        boolean kakaoMapJavascriptAvailable = hasText(kakaoMapJavascriptKey);
        boolean kakaoNotifyAvailable = kakaoNotifyEnabled
                && hasAllText(kakaoNotifyRestApiKey, kakaoRefreshToken);
        boolean googleCalendarAvailable = hasAllText(
                googleCalendarClientId,
                googleCalendarClientSecret,
                googleCalendarRedirectUri,
                googleCalendarScope
        );
        boolean googleHolidayAvailable = hasText(googleHolidayApiKey);
        boolean mailAvailable = hasAllText(mailUsername, mailPassword);
        boolean kakaoLoginAvailable = hasAllText(kakaoLoginClientId, kakaoLoginClientSecret);
        boolean naverLoginAvailable = hasAllText(naverLoginClientId, naverLoginClientSecret);

        return List.of(
                configuredService(
                        "네이버 뉴스 API",
                        "뉴스 검색 기능의 Client ID와 Client Secret 설정 상태",
                        naverNewsAvailable
                ),
                configuredService(
                        "공공데이터 API",
                        "증권·날씨·현재 시각 기능에서 사용하는 서비스 키 상태",
                        dataGoAvailable
                ),
                configuredService(
                        "카카오맵 REST API",
                        "장소 검색·주변 시설·도보·자전거·대중교통 경로 조회용 REST 키 상태",
                        kakaoMapRestAvailable
                ),
                configuredService(
                        "카카오맵 JavaScript SDK",
                        "브라우저 동적지도 표시용 JavaScript 키 상태 · 사이트 도메인은 카카오 콘솔에서 별도 확인",
                        kakaoMapJavascriptAvailable
                ),
                configuredService(
                        "구글 캘린더 API",
                        "개인 일정 연동에 필요한 OAuth Client와 Redirect URI 설정 상태",
                        googleCalendarAvailable
                ),
                configuredService(
                        "구글 공휴일 API",
                        "대한민국 공휴일 캘린더 조회용 API 키 상태",
                        googleHolidayAvailable
                ),
                configuredService(
                        "네이버 SMTP 메일",
                        "아이디·비밀번호 찾기 인증 메일 발송 계정 상태",
                        mailAvailable
                ),
                configuredService(
                        "카카오 소셜 로그인",
                        "카카오 OAuth 로그인 Client 설정 상태",
                        kakaoLoginAvailable
                ),
                configuredService(
                        "네이버 소셜 로그인",
                        "네이버 OAuth 로그인 Client 설정 상태",
                        naverLoginAvailable
                ),
                new ServiceStatusDto(
                        "카카오 상담 알림",
                        "새 상담 접수 시 관리자 카카오 알림 설정 상태",
                        kakaoNotifyAvailable ? "사용 가능" : "비활성 또는 설정 필요",
                        kakaoNotifyAvailable
                )
        );
    }

    private ServiceStatusDto configuredService(String serviceName, String description, boolean available) {
        return new ServiceStatusDto(
                serviceName,
                description,
                available ? "설정 완료" : "설정 필요",
                available
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean hasAllText(String... values) {
        if (values == null || values.length == 0) {
            return false;
        }

        for (String value : values) {
            if (!hasText(value)) {
                return false;
            }
        }
        return true;
    }
}
