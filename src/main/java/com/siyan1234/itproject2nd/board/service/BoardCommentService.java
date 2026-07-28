package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.dao.BoardCommentDao;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.siyan1234.itproject2nd.board.dao.BoardDao;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentDao boardCommentDao;
    private final BoardDao boardDao;


    // 답변 HTML의 최대 저장 길이
    // DB 컬럼은 CLOB이지만 지나치게 큰 입력을 제한하기 위한 값
    private static final int MAX_CONTENT_LENGTH = 50_000;


    // 특정 문의글의 답변 목록 조회
    public List<BoardCommentDto> findByBoardNo(Long boardNo) {

        if (boardNo == null) {
            return List.of();
        }

        return boardCommentDao.findByBoardNo(boardNo);
    }


    // 답변 한 건 조회
    public BoardCommentDto findByNo(Long no) {

        if (no == null) {
            return null;
        }

        return boardCommentDao.findByNo(no);
    }


    // 답변 등록
    @Transactional
    public int insert(BoardCommentDto commentDto) {

        if (commentDto == null) {
            throw new IllegalArgumentException(
                    "답변 정보가 올바르지 않습니다."
            );
        }

        if (commentDto.getBoardNo() == null) {
            throw new IllegalArgumentException(
                    "게시글 번호가 없습니다."
            );
        }


        String content = normalizeContent(
                commentDto.getContent()
        );

        validateContent(content);

        commentDto.setContent(content);

        int result = boardCommentDao.insert(commentDto);

        if (result != 1) {
            throw new IllegalStateException(
                    "답변 등록에 실패했습니다."
            );
        }

        /*
         * 답변 등록 후 남아 있는 답변을 기준으로
         * 답변 상태와 최근 답변 시간을 다시 계산
         */
        int statusResult =
                boardDao.refreshAnswerStatus(
                        commentDto.getBoardNo()
                );

        if (statusResult != 1) {
            throw new IllegalStateException(
                    "게시글 답변 상태와 답변 시간을 갱신하지 못했습니다."
            );
        }

        return result;
    }


    // 답변 수정
    @Transactional
    public int update(BoardCommentDto commentDto) {

        if (commentDto == null
                || commentDto.getNo() == null) {

            throw new IllegalArgumentException(
                    "수정할 답변 정보가 올바르지 않습니다."
            );
        }

        String content = normalizeContent(
                commentDto.getContent()
        );

        validateContent(content);

        commentDto.setContent(content);

        return boardCommentDao.update(commentDto);
    }


    // 답변 삭제
    @Transactional
    public int delete(Long no) {

        if (no == null) {
            throw new IllegalArgumentException(
                    "삭제할 답변 번호가 없습니다."
            );
        }

        // 삭제할 답변 조회
        BoardCommentDto commentDto =
                boardCommentDao.findByNo(no);

        if (commentDto == null) {
            throw new IllegalArgumentException(
                    "삭제할 답변을 찾을 수 없습니다."
            );
        }

        Long boardNo = commentDto.getBoardNo();

        // 답변 삭제
        int result = boardCommentDao.delete(no);

        if (result != 1) {
            throw new IllegalStateException(
                    "답변 삭제에 실패했습니다."
            );
        }

        /*
         * 답변 삭제 후 남아 있는 답변을 기준으로
         * 상태와 최근 답변 시간을 다시 계산
         */
        int statusResult =
                boardDao.refreshAnswerStatus(boardNo);

        if (statusResult != 1) {
            throw new IllegalStateException(
                    "게시글 답변 상태와 답변 시간을 갱신하지 못했습니다."
            );
        }

        return result;
    }


    // 답변 내용 정리
    private String normalizeContent(String content) {

        if (content == null) {
            return null;
        }

        return content.trim();
    }


    // 답변 내용 유효성 검사
    private void validateContent(String content) {

        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException(
                    "답변 내용을 입력해주세요."
            );
        }

        // 이미지 태그 존재 여부
        boolean hasImage =
                content.matches(
                        "(?is).*<img\\s+[^>]*src\\s*=\\s*['\"][^'\"]+['\"][^>]*>.*"
                );

        // HTML 태그를 제거하여 실제 글자 내용만 추출
        String plainText = content
                .replaceAll(
                        "(?is)<script\\b[^>]*>.*?</script>",
                        ""
                )
                .replaceAll(
                        "(?is)<style\\b[^>]*>.*?</style>",
                        ""
                )
                .replaceAll("(?is)<br\\s*/?>", "")
                .replaceAll("(?is)<[^>]+>", "")
                .replace("&nbsp;", "")
                .replace("&#160;", "")
                .replace("\u00A0", "")
                .trim();

        // 글자와 이미지가 모두 없으면 빈 답변
        if (plainText.isEmpty() && !hasImage) {
            throw new IllegalArgumentException(
                    "답변 내용을 입력해주세요."
            );
        }

        // CLOB이더라도 지나치게 큰 요청 방지
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "답변 내용은 50,000자 이하로 입력해주세요."
            );
        }
    }
}