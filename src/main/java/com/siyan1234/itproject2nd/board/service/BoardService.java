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



    // 게시글 조회
    // 조회수 증가 없이
    // 게시글만 조회
    public BoardDto findByNo(Long no) {
        return boardDao.findByNo(no);
    }




    // 게시글 페이징 조회
    // 사용자가 요청한 페이지 번호를
    // Oracle에서 사용할 시작행/끝행으로 변환

    public List<BoardDto> findPage(int page) {
        int pageSize = 10;

        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;

        return boardDao.findPage(startRow, endRow);
    }


    // 전체 게시글 개수 조회
    // 전체 게시글 개수를 조회하여
    // 전체 페이지 수 계산에 사용

    public int countAll() {
        return boardDao.countAll();
    }

    // 카테고리별 게시글 개수 조회
    public int countByCategory(String category) {
        return boardDao.countByCategory(category);
    }

    // 카테고리별 게시글 페이징 조회
    public List<BoardDto> findPageByCategory(int page, String category) {

        int pageSize = 10;

        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;

        return boardDao.findPageByCategory(
                startRow,
                endRow,
                category
        );
    }




    // 게시글 조회수 증가
    @Transactional
    public int increaseViewCount(Long no) {
        return boardDao.increaseViewCount(no);
    }

    // 게시글 작성
    @Transactional
    public int insert(BoardDto boardDto) {
        validateContent(boardDto.getContent());
        return boardDao.insert(boardDto);
    }

    // 게시글 수정
    @Transactional
    public int update(BoardDto boardDto) {
        validateContent(boardDto.getContent());
        return boardDao.update(boardDto);
    }

    // 게시글 삭제
    @Transactional
    public int delete(Long no) {
        return boardDao.delete(no);
    }

    // TOAST UI 게시글 본문 검사
    private void validateContent(String content) {

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }

        // 본문에 이미지가 있는지 확인
        boolean hasImage =
                content.matches("(?is).*<img\\s+[^>]*src=.*?>.*");

        String plainText = content
                // script와 style 내용 제거
                .replaceAll("(?is)<script.*?>.*?</script>", "")
                .replaceAll("(?is)<style.*?>.*?</style>", "")
                // 모든 HTML 태그 제거
                .replaceAll("(?s)<[^>]*>", "")
                // HTML 공백 문자 제거
                .replace("&nbsp;", "")
                .replace("&#160;", "")
                .replace("\u00A0", "")
                // 일반 공백 제거
                .trim();

        // 글자도 없고 이미지도 없을 때만 빈 본문으로 판단
        if (plainText.isEmpty() && !hasImage) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }
    }

}
