package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminDashboardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.ServiceStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 대시보드 화면에 필요한 현황 요약 데이터를 조립하는 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardDao adminDashboardDao;

    /* 뉴스 검색 API */
    @Value("${naver.client-id:}")
    private String naverNewsClientId;

    @Value("${naver.client-secret:}")
    private String naverNewsClientSecret;

    /* 증권·날씨·시간 기능에서 공동으로 사용하는 공공데이터 서비스 키 */
    @Value("${data-go.service-key:}")
    private String dataGoServiceKey;

    /* 카카오맵 REST API와 브라우저 JavaScript SDK는 서로 다른 키를 사용합니다. */
    @Value("${kakao.map.rest-api-key:}")
    private String kakaoMapRestApiKey;

    @Value("${kakao.map.javascript-key:}")
    private String kakaoMapJavascriptKey;

    /* 새 상담 접수 카카오 알림 설정 */
    @Value("${kakao.notify.enabled:false}")
    private boolean kakaoNotifyEnabled;

    @Value("${kakao.notify.rest-api-key:}")
    private String kakaoNotifyRestApiKey;

    @Value("${kakao.notify.admin-refresh-token:}")
    private String kakaoRefreshToken;

    /* 구글 캘린더 OAuth 및 대한민국 공휴일 조회 API 설정 */
    @Value("${google.calendar.client-id:}")
    private String googleCalendarClientId;

    @Value("${google.calendar.client-secret:}")
    private String googleCalendarClientSecret;

    @Value("${google.calendar.redirect-uri:}")
    private String googleCalendarRedirectUri;

    @Value("${google.calendar.scope:}")
    private String googleCalendarScope;

    @Value("${google.calendar.holiday-api-key:}")
    private String googleHolidayApiKey;

    /* 아이디·비밀번호 찾기 인증 메일 발송 설정 */
    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    /* 소셜 로그인은 뉴스 API와 별도의 OAuth 애플리케이션 설정을 사용합니다. */
    @Value("${spring.security.oauth2.client.registration.kakao.client-id:}")
    private String kakaoLoginClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret:}")
    private String kakaoLoginClientSecret;

    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverLoginClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverLoginClientSecret;

    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        AdminDashboardDto dashboard = new AdminDashboardDto();

        dashboard.setTotalMemberCount(nullToZero(adminDashboardDao.countTotalMembers()));
        dashboard.setTodayMemberCount(nullToZero(adminDashboardDao.countTodayMembers()));

        dashboard.setTotalBoardCount(nullToZero(adminDashboardDao.countTotalBoards()));
        dashboard.setTodayBoardCount(nullToZero(adminDashboardDao.countTodayBoards()));

        dashboard.setTotalChatRoomCount(nullToZero(adminDashboardDao.countTotalChatRooms()));
        dashboard.setTodayChatRoomCount(nullToZero(adminDashboardDao.countTodayChatRooms()));
        dashboard.setOpenChatRoomCount(nullToZero(adminDashboardDao.countOpenChatRooms()));
        dashboard.setClosedChatRoomCount(nullToZero(adminDashboardDao.countClosedChatRooms()));
        dashboard.setUnreadChatRoomCount(nullToZero(adminDashboardDao.countUnreadChatRooms()));

        dashboard.setTodayVisitCount(nullToZero(adminDashboardDao.countTodayVisits()));

        dashboard.setCategoryStatsList(adminDashboardDao.findCategoryStats());
        dashboard.setDailyChatStatsList(adminDashboardDao.findDailyChatStats());
        dashboard.setRecentChatRoomList(adminDashboardDao.findRecentChatRooms());
        dashboard.setServiceStatusList(createServiceStatusList());

        return dashboard;
    }

    /**
     * 외부 서비스의 비밀 값을 화면에 노출하지 않고 설정 완료 여부만 DTO로 변환합니다.
     *
     * 관리자 화면을 열 때 실제 외부 API를 호출하면 응답 지연과 호출량 증가가 발생할 수 있으므로,
     * 이 목록은 네트워크 상태가 아니라 현재 서버에 필요한 키·토큰이 등록되어 있는지를 표시합니다.
     */
    private List<ServiceStatusDto> createServiceStatusList() {
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

    /** 설정형 외부 서비스를 동일한 상태 문구로 생성합니다. */
    private ServiceStatusDto configuredService(
            String serviceName,
            String description,
            boolean available
    ) {
        return new ServiceStatusDto(
                serviceName,
                description,
                available ? "설정 완료" : "설정 필요",
                available
        );
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 여러 설정값이 모두 입력되어 있어야 하는 서비스 검증에 사용합니다. */
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
