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

    // 게시글 목록
    public List<BoardDto> findAll() {
        return boardDao.findAll();
    }

    // 카테고리별 게시글 목록
    public List<BoardDto> findByCategory(String category) {
        return boardDao.findByCategory(category);
    }

    // 게시글 검색
    public List<BoardDto> search(String keyword) {
        return boardDao.search(keyword);
    }


    public List<BoardDto> searchByCategory(
            String keyword,
            String category
    ) {

        return boardDao.searchByCategory(
                keyword,
                category
        );
    }


    // 게시글 한 개 조회
    public BoardDto findByNo(Long no) {
        return boardDao.findByNo(no);
    }

    // 전체 게시글 페이징 조회
    public List<BoardDto> findPage(int page) {

        int pageSize = 10;

        int startRow = (page - 1) * pageSize + 1;
        int endRow = page * pageSize;

        return boardDao.findPage(startRow, endRow);
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

        validateBoard(boardDto);

        // 문의글 답변 상태 기본값
        if (boardDto.getAnswerStatus() == null
                || boardDto.getAnswerStatus().isBlank()) {

            boardDto.setAnswerStatus("WAITING");
        }

        // 로그인 회원이 작성한 게시글
        if (boardDto.getWriterNo() != null) {

            boardDto.setGuestAuthorNo(null);
            boardDto.setGuestName(null);
            boardDto.setGuestPassword(null);

            return boardDao.insert(boardDto);
        }

        // 로그인하지 않은 비회원 게시글
        validateGuestAuthor(boardDto);

        GuestAuthorDto guestAuthorDto = new GuestAuthorDto();

        guestAuthorDto.setGuestName(
                boardDto.getGuestName().trim()
        );

        guestAuthorDto.setPasswordHash(
                passwordEncoder.encode(
                        boardDto.getGuestPassword()
                )
        );

        // guest_author 테이블에 비회원 정보 저장
        int guestResult =
                guestAuthorDao.insertGuestAuthor(guestAuthorDto);

        if (guestResult != 1) {
            throw new IllegalStateException(
                    "비회원 작성자 정보를 저장하지 못했습니다."
            );
        }

        /*
         * GuestAuthorMapper에서 생성된 PK를
         * guestAuthorDto.no에 넣어줘야 함
         */
        if (guestAuthorDto.getNo() == null) {
            throw new IllegalStateException(
                    "비회원 작성자 번호를 가져오지 못했습니다."
            );
        }

        // 비회원 작성자 번호를 게시글에 연결
        boardDto.setWriterNo(null);
        boardDto.setGuestAuthorNo(guestAuthorDto.getNo());


        int boardResult = boardDao.insert(boardDto);

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

    // 게시글 제목과 본문 검사
    private void validateBoard(BoardDto boardDto) {

        if (boardDto == null) {
            throw new IllegalArgumentException(
                    "게시글 정보가 없습니다."
            );
        }

        if (boardDto.getTitle() == null
                || boardDto.getTitle().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "게시글 제목을 입력해주세요."
            );
        }

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

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }

        // 본문에 이미지가 있는지 확인
        boolean hasImage =
                content.matches(
                        "(?is).*<img\\s+[^>]*src=.*?>.*"
                );

        String plainText = content
                // script와 style 내용 제거
                .replaceAll(
                        "(?is)<script.*?>.*?</script>",
                        ""
                )
                .replaceAll(
                        "(?is)<style.*?>.*?</style>",
                        ""
                )
                // 모든 HTML 태그 제거
                .replaceAll("(?s)<[^>]*>", "")
                // HTML 공백 문자 제거
                .replace("&nbsp;", "")
                .replace("&#160;", "")
                .replace("\u00A0", "")
                // 일반 공백 제거
                .trim();

        // 글자도 없고 이미지도 없을 때만 빈 본문
        if (plainText.isEmpty() && !hasImage) {
            throw new IllegalArgumentException(
                    "게시글 내용을 입력해주세요."
            );
        }
    }
}
