package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.admin.dto.CategoryStatsDto;
import com.siyan1234.itproject2nd.admin.dto.DailyChatStatsDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 대시보드 및 단일 관리자 콘솔 조회용 MyBatis Mapper 인터페이스입니다. */
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

    /** 관리자 콘솔 회원관리 목록 조회 */
    List<MemberDto> findAdminMembers(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /** 관리자 콘솔 회원관리 페이징 전체 개수 */
    Long countAdminMembers(@Param("keyword") String keyword);

    /** 관리자 콘솔 상담관리 목록 조회 */
    List<RecentChatRoomDto> findAdminChatRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    /** 관리자 콘솔 상담관리 페이징 전체 개수 */
    Long countAdminChatRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword
    );
}
