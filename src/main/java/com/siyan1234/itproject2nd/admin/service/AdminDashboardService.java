package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminDashboardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 대시보드 화면에 필요한 현황 요약 데이터를 조립하는 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final AdminDashboardDao adminDashboardDao;
    private final ExternalServiceStatusService externalServiceStatusService;

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
        dashboard.setServiceStatusList(externalServiceStatusService.getServiceStatuses());

        return dashboard;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }
}
