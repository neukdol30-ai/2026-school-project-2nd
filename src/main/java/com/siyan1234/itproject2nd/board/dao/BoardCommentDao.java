package com.siyan1234.itproject2nd.board.dao;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardCommentDao {

    // 댓글 목록 조회
    // 특정 게시글 번호(boardNo)에 해당하는 댓글들을 조회
    List<BoardCommentDto> findByBoardNo(Long boardNo);

    // 댓글 등록
    int insert(BoardCommentDto commentDto);

    // 댓글 수정
    int update(BoardCommentDto commentDto);

    // 댓글 삭제
    int delete(Long no);

    // 댓글 1개 조회
    BoardCommentDto findByNo(Long no);

}
