package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.dao.BoardDao;
import com.siyan1234.itproject2nd.board.dao.GuestAuthorDao;
import com.siyan1234.itproject2nd.board.dto.BoardDto;
import com.siyan1234.itproject2nd.board.dto.GuestAuthorDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardDao boardDao;
    private final GuestAuthorDao guestAuthorDao;
    private final PasswordEncoder passwordEncoder;

    /*
     * 로그인 회원 게시글 작성 횟수 제한
     */
    private final BoardWriteRateLimitService
            boardWriteRateLimitService;

    // 비회원 문의글 답변 완료 후 보관 기간
    private static final int GUEST_BOARD_RETENTION_DAYS = 30;

    // 게시글 한 페이지 표시 개수
    private static final int PAGE_SIZE = 10;

    // 게시글 제목 최대 글자 수
    private static final int BOARD_TITLE_MAX_LENGTH = 200;

    // HTML 태그를 제외한 본문 실제 최대 글자 수
    private static final int BOARD_CONTENT_TEXT_MAX_LENGTH = 10_000;

    // HTML 태그를 포함한 본문 전체 최대 길이
    private static final int BOARD_CONTENT_HTML_MAX_LENGTH = 30_000;

    // 게시글 목록
    public List<BoardDto> findAll() {
        return boardDao.findAll();
    }

    // 카테고리별 게시글 목록
    public List<BoardDto> findByCategory(String category) {
        return boardDao.findByCategory(category);
    }

    /*
     * 전체 게시글 검색 결과 페이징 조회
     */
    public List<BoardDto> search(
            String keyword,
            int page
    ) {
        int safePage = Math.max(page, 1);

        int startRow =
                (safePage - 1) * PAGE_SIZE + 1;

        int endRow =
                safePage * PAGE_SIZE;

        String cleanKeyword =
                cleanSearchKeyword(keyword);

        return boardDao.search(
                cleanKeyword,
                startRow,
                endRow
        );
    }


    /*
     * 전체 게시글 검색 결과 개수
     */
    public int countSearch(String keyword) {

        String cleanKeyword =
                cleanSearchKeyword(keyword);

        return boardDao.countSearch(
                cleanKeyword
        );
    }


    /*
     * 카테고리별 게시글 검색 결과 페이징 조회
     */
    public List<BoardDto> searchByCategory(
            String keyword,
            String category,
            int page
    ) {
        int safePage = Math.max(page, 1);

        int startRow =
                (safePage - 1) * PAGE_SIZE + 1;

        int endRow =
                safePage * PAGE_SIZE;

        String cleanKeyword =
                cleanSearchKeyword(keyword);

        String cleanCategory =
                category == null
                        ? ""
                        : category.trim();

        /*
         * BoardDao의 매개변수 순서:
         * category, keyword, startRow, endRow
         */
        return boardDao.searchByCategory(
                cleanCategory,
                cleanKeyword,
                startRow,
                endRow
        );
    }


    /*
     * 카테고리별 게시글 검색 결과 개수
     */
    public int countSearchByCategory(
            String keyword,
            String category
    ) {
        String cleanKeyword =
                cleanSearchKeyword(keyword);

        String cleanCategory =
                category == null
                        ? ""
                        : category.trim();

        return boardDao.countSearchByCategory(
                cleanCategory,
                cleanKeyword
        );
    }


    /*
     * 게시글 전체 페이지 수 계산
     */
    public int calculateTotalPage(int totalCount) {

        if (totalCount <= 0) {
            return 1;
        }

        return (int) Math.ceil(
                (double) totalCount / PAGE_SIZE
        );
    }


    // 게시글 한 개 조회
    public BoardDto findByNo(Long no) {
        return boardDao.findByNo(no);
    }

    // 전체 게시글 페이징 조회
    public List<BoardDto> findPage(int page) {

        int safePage = Math.max(page, 1);

        int startRow =
                (safePage - 1) * PAGE_SIZE + 1;

        int endRow =
                safePage * PAGE_SIZE;

        return boardDao.findPage(
                startRow,
                endRow
        );
    }

    // 전체 게시글 개수
    public int countAll() {
        return boardDao.countAll();
    }

    // 카테고리별 게시글 개수
    public int countByCategory(String category) {
        return boardDao.countByCategory(category);
    }

    // 카테고리별 게시글 페이징 조회
    public List<BoardDto> findPageByCategory(
            int page,
            String category
    ) {
        int safePage = Math.max(page, 1);

        int startRow =
                (safePage - 1) * PAGE_SIZE + 1;

        int endRow =
                safePage * PAGE_SIZE;

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

    /*
     * 게시글 작성
     *
     * 회원 게시글:
     * writerNo에 회원 번호가 들어 있음
     * guestAuthorNo는 null
     *
     * 비회원 게시글:
     * writerNo가 null
     * guest_author를 먼저 저장한 뒤
     * 생성된 번호를 guestAuthorNo에 저장
     */
    @Transactional
    public int insert(BoardDto boardDto) {

        /*
         * 게시글 제목과 본문 검사
         */
        validateBoard(boardDto);

        /*
         * 문의글 답변 상태 기본값
         */
        if (boardDto.getAnswerStatus() == null
                || boardDto.getAnswerStatus().isBlank()) {

            boardDto.setAnswerStatus("WAITING");
        }

        /*
         * 로그인 회원이 작성한 게시글
         */
        if (boardDto.getWriterNo() != null) {

            /*
             * DB에 저장하기 전에
             * 회원 번호 기준으로 작성 횟수 검사
             */
            boardWriteRateLimitService.validateWrite(
                    boardDto.getWriterNo().longValue()
            );

            /*
             * 로그인 회원 게시글에는
             * 비회원 정보가 저장되지 않도록 초기화
             */
            boardDto.setGuestAuthorNo(null);
            boardDto.setGuestName(null);
            boardDto.setGuestPassword(null);

            int boardResult =
                    boardDao.insert(boardDto);

            if (boardResult != 1) {
                throw new IllegalStateException(
                        "게시글을 저장하지 못했습니다."
                );
            }

            return boardResult;
        }

        /*
         * 로그인하지 않은 비회원 게시글
         */
        validateGuestAuthor(boardDto);

        GuestAuthorDto guestAuthorDto =
                new GuestAuthorDto();

        guestAuthorDto.setGuestName(
                boardDto.getGuestName().trim()
        );

        guestAuthorDto.setPasswordHash(
                passwordEncoder.encode(
                        boardDto.getGuestPassword()
                )
        );

        /*
         * guest_author 테이블에 비회원 정보 저장
         */
        int guestResult =
                guestAuthorDao.insertGuestAuthor(
                        guestAuthorDto
                );

        if (guestResult != 1) {
            throw new IllegalStateException(
                    "비회원 작성자 정보를 저장하지 못했습니다."
            );
        }

        /*
         * 생성된 비회원 작성자 번호 확인
         */
        if (guestAuthorDto.getNo() == null) {
            throw new IllegalStateException(
                    "비회원 작성자 번호를 가져오지 못했습니다."
            );
        }

        /*
         * 비회원 작성자 번호를 게시글에 연결
         */
        boardDto.setWriterNo(null);
        boardDto.setGuestAuthorNo(
                guestAuthorDto.getNo()
        );

        /*
         * 게시글 DB 저장
         */
        int boardResult =
                boardDao.insert(boardDto);

        if (boardResult != 1) {
            throw new IllegalStateException(
                    "게시글을 저장하지 못했습니다."
            );
        }

        return boardResult;
    }

    // 게시글 수정
    @Transactional
    public int update(BoardDto boardDto) {

        validateBoard(boardDto);

        return boardDao.update(boardDto);
    }

    /*
     * 게시글 삭제
     *
     * 비회원 게시글이라면:
     * 1. board 삭제
     * 2. guest_author 삭제
     *
     * 하나의 트랜잭션으로 실행됨
     */
    @Transactional
    public int delete(Long no) {

        BoardDto boardDto = boardDao.findByNo(no);

        if (boardDto == null) {
            throw new IllegalArgumentException(
                    "존재하지 않는 게시글입니다."
            );
        }

        Long guestAuthorNo =
                boardDto.getGuestAuthorNo();

        int result = boardDao.delete(no);

        if (result != 1) {
            throw new IllegalStateException(
                    "게시글을 삭제하지 못했습니다."
            );
        }

        // 비회원 게시글인 경우 비회원 작성자 정보도 삭제
        if (guestAuthorNo != null) {

            int guestResult =
                    guestAuthorDao.deleteByNo(guestAuthorNo);

            if (guestResult != 1) {
                throw new IllegalStateException(
                        "비회원 작성자 정보를 삭제하지 못했습니다."
                );
            }
        }

        return result;
    }

    /*
     * 답변 완료 후 30일이 지난
     * 비회원 문의글 자동 삭제
     */
    @Transactional
    public int deleteExpiredGuestBoards() {

        // 자동 삭제 대상 게시글 번호 조회
        List<Long> expiredBoardNos =
                boardDao.findExpiredGuestBoardNos(
                        GUEST_BOARD_RETENTION_DAYS
                );

        // 삭제 대상이 없으면 0 반환
        if (expiredBoardNos == null
                || expiredBoardNos.isEmpty()) {

            return 0;
        }

        int deletedCount = 0;

        for (Long boardNo : expiredBoardNos) {

            if (boardNo == null) {
                continue;
            }

            /*
             * 기존 게시글 삭제 메서드를 재사용한다.
             *
             * board 삭제
             * → board_comment는 ON DELETE CASCADE로 삭제
             * → guest_author도 기존 delete()에서 삭제
             */
            delete(boardNo);

            deletedCount++;
        }

        return deletedCount;
    }

    /*
     * 검색어 앞뒤 공백 제거
     */
    private String cleanSearchKeyword(String keyword) {

        if (keyword == null) {
            return "";
        }

        return keyword.trim();
    }


    // 게시글 제목과 본문 검사
    private void validateBoard(BoardDto boardDto) {

        if (boardDto == null) {
            throw new IllegalArgumentException(
                    "게시글 정보가 없습니다."
            );
        }

        String title =
                boardDto.getTitle() == null
                        ? ""
                        : boardDto.getTitle().trim();

        // 제목 입력 여부
        if (title.isEmpty()) {
            throw new IllegalArgumentException(
                    "게시글 제목을 입력해주세요."
            );
        }

        // 제목 200자 제한
        if (title.length() > BOARD_TITLE_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "제목은 200자 이하로 입력해주세요."
            );
        }

        // 앞뒤 공백을 제거한 제목으로 다시 저장
        boardDto.setTitle(title);

        // 본문 검사
        validateContent(boardDto.getContent());
    }

    // 비회원 작성자 정보 검사
    private void validateGuestAuthor(BoardDto boardDto) {

        // 비회원 이름 검사
        if (boardDto.getGuestName() == null
                || boardDto.getGuestName().isBlank()) {

            throw new IllegalArgumentException(
                    "비회원 이름을 입력해주세요."
            );
        }

        String guestPassword =
                boardDto.getGuestPassword();

        // 비밀번호 입력 여부 검사
        if (guestPassword == null
                || guestPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "비회원 비밀번호를 입력해주세요."
            );
        }

        // 비밀번호 길이 검사
        if (guestPassword.length() < 4
                || guestPassword.length() > 20) {

            throw new IllegalArgumentException(
                    "비회원 비밀번호는 4자 이상 20자 이하로 입력해주세요."
            );
        }
    }

    public boolean checkGuestPassword(
            Long boardNo,
            String guestPassword
    ) {

        if (guestPassword == null
                || guestPassword.isBlank()) {

            return false;
        }


        BoardDto boardDto = boardDao.findByNo(boardNo);

        if (boardDto == null ||
                boardDto.getGuestAuthorNo() == null) {

            return false;
        }

        GuestAuthorDto guestAuthor =
                guestAuthorDao.findByNo(
                        boardDto.getGuestAuthorNo()
                );

        if (guestAuthor == null) {
            return false;
        }

        return passwordEncoder.matches(
                guestPassword,
                guestAuthor.getPasswordHash()
        );
    }


    @Transactional
    public void updateGuestBoard(
            BoardDto boardDto
    ) {

        BoardDto originBoard =
                boardDao.findByNo(
                        boardDto.getNo()
                );

        if (originBoard == null) {
            throw new IllegalArgumentException(
                    "게시글을 찾을 수 없습니다."
            );
        }

        if (originBoard.getGuestAuthorNo() == null) {
            throw new IllegalArgumentException(
                    "비회원 게시글이 아닙니다."
            );
        }

        /*
         * 제목과 본문 길이 검사
         */
        validateBoard(boardDto);

        String title =
                boardDto.getTitle() == null
                        ? ""
                        : boardDto.getTitle().trim();

        String content =
                boardDto.getContent() == null
                        ? ""
                        : boardDto.getContent().trim();

        if (title.isBlank()) {
            throw new IllegalArgumentException(
                    "제목을 입력해주세요."
            );
        }

        if (content.isBlank()) {
            throw new IllegalArgumentException(
                    "내용을 입력해주세요."
            );
        }

        /*
         * 비회원 수정에서는
         * 제목, 내용, 카테고리만 사용
         */
        BoardDto updateBoard =
                new BoardDto();

        updateBoard.setNo(
                originBoard.getNo()
        );

        updateBoard.setTitle(title);
        updateBoard.setContent(content);
        updateBoard.setCategory("QUESTION");

        int result = boardDao.update(updateBoard);

        if (result != 1) {
            throw new IllegalStateException(
                    "게시글을 수정하지 못했습니다."
            );
        }
    }



    // TOAST UI 게시글 본문 검사
    private void validateContent(String content) {

        /*
         * 전달된 값 자체가 없는 경우
         */
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }

        /*
         * HTML 태그를 포함한 전체 문자열 제한
         */
        if (content.length()
                > BOARD_CONTENT_HTML_MAX_LENGTH) {

            throw new IllegalArgumentException(
                    "게시글 내용에 너무 많은 서식이 포함되어 있습니다."
            );
        }

        /*
         * 본문에 이미지가 있는지 확인
         */
        boolean hasImage =
                content.matches(
                        "(?is).*<img\\s+[^>]*src=.*?>.*"
                );

        /*
         * 화면에 실제로 표시되는 글자 추출
         */
        String plainText = content

                // script 내용 제거
                .replaceAll(
                        "(?is)<script.*?>.*?</script>",
                        ""
                )

                // style 내용 제거
                .replaceAll(
                        "(?is)<style.*?>.*?</style>",
                        ""
                )

                // 모든 HTML 태그 제거
                .replaceAll(
                        "(?s)<[^>]*>",
                        ""
                )

                // HTML 공백 문자 변환
                .replaceAll(
                        "(?i)&nbsp;|&#160;|&#xA0;",
                        " "
                )

                .replace("\u00A0", " ")
                .trim();

        /*
         * 글자와 이미지가 모두 없는 경우
         */
        if (plainText.isEmpty() && !hasImage) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }

        /*
         * 화면에 보이는 실제 글자 수 제한
         */
        if (plainText.length()
                > BOARD_CONTENT_TEXT_MAX_LENGTH) {

            throw new IllegalArgumentException(
                    "본문은 10,000자 이하로 입력해주세요."
            );
        }
    }
}
