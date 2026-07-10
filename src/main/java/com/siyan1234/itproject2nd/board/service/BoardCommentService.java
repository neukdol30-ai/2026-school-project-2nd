package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.dao.BoardCommentDao;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentDao boardCommentDao;

    // 댓글 목록 조회
    public List<BoardCommentDto> findByBoardNo(Long boardNo) {
        return boardCommentDao.findByBoardNo(boardNo);
    }

    // 댓글 1개 조회
    public BoardCommentDto findByNo(Long no) {
        return boardCommentDao.findByNo(no);
    }

    // 댓글 등록
    @Transactional
    public int insert(BoardCommentDto commentDto) {
        return boardCommentDao.insert(commentDto);
    }

    // 댓글 수정
    @Transactional
    public int update(BoardCommentDto commentDto) {
        return boardCommentDao.update(commentDto);
    }

    // 댓글 삭제
    @Transactional
    public int delete(Long no) {
        return boardCommentDao.delete(no);
    }
}
