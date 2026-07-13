package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminDashboardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.ServiceStatusDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 대시보드 화면에 필요한 통계 데이터를 조립하는 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardDao adminDashboardDao;

    @Value("${naver.client-id:}")
    private String naverClientId;

    @Value("${naver.client-secret:}")
    private String naverClientSecret;

    @Value("${data-go.service-key:}")
    private String dataGoServiceKey;

    @Value("${kakao.notify.enabled:false}")
    private boolean kakaoNotifyEnabled;

    @Value("${kakao.notify.rest-api-key:}")
    private String kakaoRestApiKey;

    @Value("${kakao.notify.admin-refresh-token:}")
    private String kakaoRefreshToken;

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

    private List<ServiceStatusDto> createServiceStatusList() {
        boolean naverAvailable = hasText(naverClientId) && hasText(naverClientSecret);
        boolean dataGoAvailable = hasText(dataGoServiceKey);
        boolean kakaoAvailable = kakaoNotifyEnabled && hasText(kakaoRestApiKey) && hasText(kakaoRefreshToken);

        return List.of(
                new ServiceStatusDto(
                        "네이버 뉴스 API",
                        "뉴스 검색 서비스 연결 상태",
                        naverAvailable ? "설정 완료" : "설정 필요",
                        naverAvailable
                ),
                new ServiceStatusDto(
                        "공공데이터 API",
                        "증권/외부 데이터 서비스 키 상태",
                        dataGoAvailable ? "설정 완료" : "설정 필요",
                        dataGoAvailable
                ),
                new ServiceStatusDto(
                        "카카오 상담 알림",
                        "새 상담 접수 시 관리자 카카오 알림 상태",
                        kakaoAvailable ? "사용 가능" : "비활성 또는 설정 필요",
                        kakaoAvailable
                )
        );
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
