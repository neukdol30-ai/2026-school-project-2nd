package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 콘솔 상담 관리 조회용 MyBatis Mapper입니다. */
@Mapper
public interface AdminChatDao {

    List<RecentChatRoomDto> findAdminChatRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countAdminChatRooms(
            @Param("status") String status,
            @Param("category") String category,
            @Param("keyword") String keyword
    );
}
