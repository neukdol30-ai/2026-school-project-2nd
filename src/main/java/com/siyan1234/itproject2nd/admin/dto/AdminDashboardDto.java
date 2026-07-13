package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 관리자 대시보드 화면에 필요한 모든 데이터를 한 번에 담는 DTO입니다.
 * Controller는 이 DTO 하나를 Model에 담아 dashboard.html로 전달합니다.
 */
@Getter
@Setter
public class AdminDashboardDto {

    private Long totalMemberCount;
    private Long todayMemberCount;

    private Long totalBoardCount;
    private Long todayBoardCount;

    private Long totalChatRoomCount;
    private Long todayChatRoomCount;
    private Long openChatRoomCount;
    private Long closedChatRoomCount;
    private Long unreadChatRoomCount;

    private Long todayVisitCount;

    private List<CategoryStatsDto> categoryStatsList;
    private List<DailyChatStatsDto> dailyChatStatsList;
    private List<RecentChatRoomDto> recentChatRoomList;
    private List<ServiceStatusDto> serviceStatusList;

    public void setTotalBoardO() {

    }
}
