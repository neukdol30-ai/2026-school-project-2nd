package com.siyan1234.itproject2nd.board.dao;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardCommentDao {


    // 특정 문의글의 답변 목록 조회
    List<BoardCommentDto> findByBoardNo(Long boardNo);


    // 답변 한 건 조회
    BoardCommentDto findByNo(Long no);


    // 답변 등록
    int insert(BoardCommentDto commentDto);


    // 답변 수정
    int update(BoardCommentDto commentDto);

    // 답변 삭제
    int delete(Long no);

}
