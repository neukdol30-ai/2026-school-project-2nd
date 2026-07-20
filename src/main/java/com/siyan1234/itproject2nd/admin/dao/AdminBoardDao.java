package com.siyan1234.itproject2nd.admin.dao;

import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 관리자 콘솔 게시글 관리 Mapper 인터페이스입니다. */
@Mapper
public interface AdminBoardDao {

    List<AdminBoardDto> findAdminBoards(
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size
    );

    Long countAdminBoards(
            @Param("category") String category,
            @Param("keyword") String keyword
    );

    AdminBoardDto findByNo(@Param("boardNo") Long boardNo);

    int insertBoard(AdminBoardDto boardDto);

    int updateBoard(AdminBoardDto boardDto);

    int deleteBoard(@Param("boardNo") Long boardNo);
}
