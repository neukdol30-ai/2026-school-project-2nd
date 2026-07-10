package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.dao.BoardDao;
import com.siyan1234.itproject2nd.board.dto.BoardDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardDao boardDao;

    // 게시글 목록
    public List<BoardDto> findAll() {
        return boardDao.findAll();
    }

    // 카테고리별 게시글 목록
    public List<BoardDto> findByCategory(String category) {
        return boardDao.findByCategory(category);
    }

    // 게시글 검색
    // 사용자가 입력한 검색어를 DAO로 전달하여
    // 검색 결과 목록을 반환
    public List<BoardDto> search(String keyword) {
        return boardDao.search(keyword);
    }


    // =========================
    // 게시글 조회
    // =========================
    //
    // 조회수 증가 없이
    // 게시글만 조회
    //
    public BoardDto findByNo(Long no) {
        return boardDao.findByNo(no);
    }



    // =========================
    // 게시글 페이징 조회
    // =========================
    //
    // 사용자가 요청한 페이지 번호를
    // Oracle에서 사용할 시작행/끝행으로 변환
    //
    // 예)
    // page = 1
    // startRow = 1
    // endRow = 10
    //
    // page = 2
    // startRow = 11
    // endRow = 20
    //
    public List<BoardDto> findPage(int page) {
        int pageSize = 10;

        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;

        return boardDao.findPage(startRow, endRow);
    }

    // =========================
    // 전체 게시글 개수 조회
    // =========================
    //
    // 전체 게시글 개수를 조회하여
    // 전체 페이지 수 계산에 사용
    //
    public int countAll() {
        return boardDao.countAll();
    }


    // =========================
    // 게시글 조회수 증가
    // =========================
    //
    // 게시글 상세보기에 처음 들어왔을 때
    // 조회수를 1 증가시키는 메서드
    //
    // Controller에서 세션을 확인한 후
    // 처음 조회한 경우에만 호출됨
    //
    // 게시글 번호(no)를 받아
    // BoardDao의 increaseViewCount()를 실행
    //
    @Transactional
    public int increaseViewCount(Long no) {
        return boardDao.increaseViewCount(no);
    }

    // 게시글 작성
    @Transactional
    public int insert(BoardDto boardDto) {
        return boardDao.insert(boardDto);
    }

    // 게시글 수정
    @Transactional
    public int update(BoardDto boardDto) {
        return boardDao.update(boardDto);
    }

    // 게시글 삭제
    @Transactional
    public int delete(Long no) {
        return boardDao.delete(no);
    }
}
