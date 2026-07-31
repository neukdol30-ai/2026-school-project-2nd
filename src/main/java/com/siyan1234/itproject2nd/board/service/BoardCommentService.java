package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.dao.BoardCommentDao;
import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import com.siyan1234.itproject2nd.board.dao.BoardDao;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardCommentService {

    private final BoardCommentDao boardCommentDao;
    private final BoardDao boardDao;


    /*
     * 답변에 실제로 표시되는 글자 수 제한
     */
    private static final int MAX_TEXT_LENGTH = 1_000;

    /*
     * Toast UI가 생성하는 HTML 전체 길이 제한
     */
    private static final int MAX_HTML_LENGTH = 30_000;


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


    /*
     * 답변 내용 유효성 검사
     */
    private void validateContent(String content) {

        /*
         * null 또는 완전히 빈 문자열 검사
         */
        if (content == null
                || content.isBlank()) {

            throw new IllegalArgumentException(
                    "답변 내용을 입력해주세요."
            );
        }

        /*
         * 지나치게 큰 HTML 요청을 먼저 차단한다.
         *
         * CLOB 컬럼이어도 무제한으로 입력받으면
         * 서버와 DB에 부담이 생길 수 있다.
         */
        if (
                content.length()
                        > MAX_HTML_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "답변에 너무 많은 서식이 포함되어 있습니다."
            );
        }

        /*
         * 이미지 태그 존재 여부
         *
         * 글자는 없어도 이미지만 등록된 답변은
         * 유효한 답변으로 허용한다.
         */
        boolean hasImage =
                content.matches(
                        "(?is).*<img\\s+[^>]*src\\s*=\\s*['\"][^'\"]+['\"][^>]*>.*"
                );

        /*
         * 저장된 HTML에서 script와 style 제거
         */
        String plainText =
                content
                        .replaceAll(
                                "(?is)<script\\b[^>]*>.*?</script>",
                                ""
                        )
                        .replaceAll(
                                "(?is)<style\\b[^>]*>.*?</style>",
                                ""
                        )
                        .replaceAll(
                                "(?is)<br\\s*/?>",
                                ""
                        )
                        .replaceAll(
                                "(?is)<[^>]+>",
                                ""
                        );

        /*
         * &amp;, &lt;, &nbsp; 등의 HTML 문자를
         * 실제 화면에 표시되는 문자로 변환한다.
         */
        plainText =
                HtmlUtils
                        .htmlUnescape(plainText)
                        .replace(
                                '\u00A0',
                                ' '
                        )
                        .trim();

        /*
         * 실제 글자와 이미지가 모두 없으면
         * 빈 답변으로 처리한다.
         */
        if (
                plainText.isEmpty()
                        && !hasImage
        ) {
            throw new IllegalArgumentException(
                    "답변 내용을 입력해주세요."
            );
        }

        /*
         * 화면에 표시되는 실제 답변 글자 수 검사
         */
        if (
                plainText.length()
                        > MAX_TEXT_LENGTH
        ) {
            throw new IllegalArgumentException(
                    "답변은 "
                            + MAX_TEXT_LENGTH
                            + "자 이하로 입력해주세요."
            );
        }
    }
}