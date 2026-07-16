package com.siyan1234.itproject2nd.board.dao;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface BoardCommentDao {

    // =========================
    // 특정 문의글의 답변 목록 조회
    // =========================
    //
    // boardNo에 해당하는 모든 답변을 조회한다.
    // BoardCommentMapper.xml의 findByBoardNo와 연결된다.
    //
    List<BoardCommentDto> findByBoardNo(Long boardNo);

    // =========================
    // 답변 한 건 조회
    // =========================
    //
    // 답변 수정·삭제 시 원본 답변과 작성자를 확인하기 위해 사용한다.
    // BoardCommentMapper.xml의 findByNo와 연결된다.
    //
    BoardCommentDto findByNo(Long no);

    // =========================
    // 답변 등록
    // =========================
    //
    // 문의글에 새 답변을 저장한다.
    // content에는 TOAST UI에서 작성한 HTML이 전달된다.
    // BoardCommentMapper.xml의 insert와 연결된다.
    //
    int insert(BoardCommentDto commentDto);

    // =========================
    // 답변 수정
    // =========================
    //
    // 답변 번호와 수정된 HTML 내용을 이용해 기존 답변을 수정한다.
    // BoardCommentMapper.xml의 update와 연결된다.
    //
    int update(BoardCommentDto commentDto);

    // =========================
    // 답변 삭제
    // =========================
    //
    // 답변 번호에 해당하는 답변을 삭제한다.
    // BoardCommentMapper.xml의 delete와 연결된다.
    //
    int delete(Long no);

}
