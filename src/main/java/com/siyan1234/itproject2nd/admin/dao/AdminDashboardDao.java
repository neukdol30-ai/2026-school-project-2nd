package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.admin.dto.CategoryStatsDto;
import com.siyan1234.itproject2nd.admin.dto.DailyChatStatsDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 관리자 대시보드 통계 조회용 MyBatis Mapper입니다. */
@Mapper
public interface AdminDashboardDao {

    Long countTotalMembers();

    Long countTodayMembers();

    Long countTotalBoards();

    Long countTodayBoards();

    Long countTotalChatRooms();

    Long countTodayChatRooms();

    Long countOpenChatRooms();

    Long countClosedChatRooms();

    Long countUnreadChatRooms();

    Long countTodayVisits();

    List<CategoryStatsDto> findCategoryStats();

    List<DailyChatStatsDto> findDailyChatStats();

    List<RecentChatRoomDto> findRecentChatRooms();
}
