package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminDashboardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.dto.ServiceStatusDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
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
    private final ChatRedisService chatRedisService;

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

    @Transactional(readOnly = true)
    public List<MemberDto> findAdminMembers(String keyword, int page, int size) {
        int offset = calculateOffset(page, size);
        return adminDashboardDao.findAdminMembers(cleanText(keyword), offset, size);
    }

    @Transactional(readOnly = true)
    public long countAdminMembers(String keyword) {
        return nullToZero(adminDashboardDao.countAdminMembers(cleanText(keyword)));
    }

    @Transactional(readOnly = true)
    public List<RecentChatRoomDto> findAdminChatRooms(
            String status,
            String category,
            String keyword,
            int page,
            int size
    ) {
        return findAdminChatRooms(status, category, keyword, null, page, size);
    }

    /**
     * 관리자 콘솔 상담관리 목록 조회입니다.
     *
     * viewerNo가 전달되면 Redis에 아직 DB 저장 전인 메시지의 안읽음 개수까지 합산합니다.
     * /admin?view=chats 실시간 목록 갱신에서도 이 메서드를 사용해서
     * 관리자 콘솔과 채팅 모듈의 데이터를 같은 기준으로 보여줍니다.
     */
    @Transactional(readOnly = true)
    public List<RecentChatRoomDto> findAdminChatRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    ) {
        int offset = calculateOffset(page, size);

        List<RecentChatRoomDto> roomList = adminDashboardDao.findAdminChatRooms(
                cleanText(status),
                cleanText(category),
                cleanText(keyword),
                offset,
                size
        );

        if (viewerNo == null) {
            return roomList;
        }

        for (RecentChatRoomDto room : roomList) {
            long dbUnreadCount = room.getUnreadCount() == null ? 0L : room.getUnreadCount();
            int redisUnreadCount = chatRedisService.countUnreadMessages(room.getRoomNo(), viewerNo);
            room.setUnreadCount(dbUnreadCount + redisUnreadCount);
        }

        return roomList;
    }

    @Transactional(readOnly = true)
    public long countAdminChatRooms(String status, String category, String keyword) {
        return nullToZero(adminDashboardDao.countAdminChatRooms(
                cleanText(status),
                cleanText(category),
                cleanText(keyword)
        ));
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

    private int calculateOffset(int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        return (safePage - 1) * safeSize;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
