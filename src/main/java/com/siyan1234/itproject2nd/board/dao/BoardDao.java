package com.siyan1234.itproject2nd.board.dao;

import com.siyan1234.itproject2nd.board.dto.BoardDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardDao {

    // 게시글 목록
    List<BoardDto> findAll();

    // 카테고리별 게시글 목록
    List<BoardDto> findByCategory(String category);

    // 카테고리별 게시글 개수 조회
    int countByCategory(String category);

    // 카테고리별 게시글 페이징 조회
    List<BoardDto> findPageByCategory(
            @Param("startRow") int startRow,
            @Param("endRow") int endRow,
            @Param("category") String category
    );

    // 게시글 검색
    // 제목, 내용, 작성자 닉네임을 기준으로 검색
    List<BoardDto> search(String keyword);


    List<BoardDto> searchByCategory(
            @Param("keyword") String keyword,
            @Param("category") String category
    );


    // 게시글 상세
    BoardDto findByNo(Long no);

    // 게시글 작성
    int insert(BoardDto boardDto);

    // 게시글 전체 개수 조회
    // board 테이블에 저장된
    // 전체 게시글 개수를 조회
    // 페이징에서
    // 전체 페이지 수를 계산하기 위해 사용
    int countAll();


    // 게시글 페이징 조회
    // startRow : 조회 시작 행
    // endRow   : 조회 마지막 행
    // Oracle ROWNUM을 이용하여
    // 한 페이지에 필요한 게시글만 조회
    List<BoardDto> findPage(@Param("startRow") int startRow,
                            @Param("endRow") int endRow);

    // 조회수 증가
    int increaseViewCount(Long no);

    // 게시글 수정
    int update(BoardDto boardDto);

    // 게시글 삭제
    int delete(Long no);

    // 게시글 답변 상태 변경
    int updateAnswerStatus(
            @Param("boardNo") Long boardNo,
            @Param("answerStatus") String answerStatus
    );

    // 게시글에 등록된 답변 개수 조회
    int countCommentsByBoardNo(Long boardNo);


    // 해당 게시글의 답변 개수를 확인해
    // 답변 상태와 최근 답변 시간을 다시 계산한다.
    int refreshAnswerStatus(Long boardNo);

}
