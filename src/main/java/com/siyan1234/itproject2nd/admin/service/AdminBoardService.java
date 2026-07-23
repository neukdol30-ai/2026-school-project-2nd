package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminBoardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;

import com.siyan1234.itproject2nd.board.dto.BoardCommentDto;
import com.siyan1234.itproject2nd.board.service.BoardCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 게시글 관리 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminBoardService {

    private static final String CATEGORY_NOTICE = "NOTICE";
    private static final String CATEGORY_QUESTION = "QUESTION";
    private static final String ANSWER_WAITING = "WAITING";
    private static final String ANSWER_ANSWERED = "ANSWERED";

    private final AdminBoardDao adminBoardDao;
    private final BoardCommentService boardCommentService;

    @Transactional(readOnly = true)
    public List<AdminBoardDto> findBoards(String category, String keyword, int page, int size) {
        int offset = AdminPagingHelper.calculateOffset(page, size);
        return adminBoardDao.findAdminBoards(
                AdminPagingHelper.cleanText(category),
                AdminPagingHelper.cleanText(keyword),
                offset,
                size
        );
    }

    @Transactional(readOnly = true)
    public long countBoards(String category, String keyword) {
        Long count = adminBoardDao.countAdminBoards(
                AdminPagingHelper.cleanText(category),
                AdminPagingHelper.cleanText(keyword)
        );
        return count == null ? 0L : count;
    }

    @Transactional(readOnly = true)
    public AdminBoardDto findByNo(Long boardNo) {
        if (boardNo == null) {
            return null;
        }
        return adminBoardDao.findByNo(boardNo);
    }

    @Transactional
    public int createBoard(AdminBoardDto boardDto, Integer writerNo) {
        prepareForSave(boardDto);
        boardDto.setWriterNo(writerNo);
        return adminBoardDao.insertBoard(boardDto);
    }

    @Transactional
    public int updateBoard(AdminBoardDto boardDto) {
        if (boardDto == null || boardDto.getNo() == null) {
            return 0;
        }
        prepareForSave(boardDto);
        return adminBoardDao.updateBoard(boardDto);
    }


    @Transactional
    public int createAnswer(Long boardNo, BoardCommentDto commentDto, Integer writerNo) {
        AdminBoardDto board = validateAnswerTarget(boardNo);

        if (commentDto == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_ANSWER_CONTENT_REQUIRED);
        }

        commentDto.setBoardNo(board.getNo());
        commentDto.setWriterNo(writerNo);

        int insertedCount = boardCommentService.insert(commentDto);
        if (insertedCount > 0) {
            adminBoardDao.updateAnswerStatus(boardNo, ANSWER_ANSWERED);
        }
        return insertedCount;
    }

    @Transactional
    public int updateAnswer(Long boardNo, Long answerNo, BoardCommentDto commentDto) {
        validateAnswerTarget(boardNo);

        BoardCommentDto origin = boardCommentService.findByNo(answerNo);
        if (origin == null || !boardNo.equals(origin.getBoardNo())) {
            return 0;
        }

        commentDto.setNo(answerNo);
        commentDto.setBoardNo(boardNo);
        commentDto.setWriterNo(origin.getWriterNo());

        int updatedCount = boardCommentService.update(commentDto);
        if (updatedCount > 0) {
            adminBoardDao.updateAnswerStatus(boardNo, ANSWER_ANSWERED);
        }
        return updatedCount;
    }

    @Transactional
    public int deleteAnswer(Long boardNo, Long answerNo) {
        validateAnswerTarget(boardNo);

        BoardCommentDto origin = boardCommentService.findByNo(answerNo);
        if (origin == null || !boardNo.equals(origin.getBoardNo())) {
            return 0;
        }

        int deletedCount = boardCommentService.delete(answerNo);
        if (deletedCount > 0) {
            long answerCount = adminBoardDao.countBoardAnswers(boardNo);
            adminBoardDao.updateAnswerStatus(boardNo, answerCount > 0 ? ANSWER_ANSWERED : ANSWER_WAITING);
        }
        return deletedCount;
    }

    @Transactional
    public int deleteBoard(Long boardNo) {
        if (boardNo == null) {
            return 0;
        }
        return adminBoardDao.deleteBoard(boardNo);
    }

    @Transactional
    public AdminDeleteResultDto deleteBoards(List<Long> boardNoList) {
        if (boardNoList == null || boardNoList.isEmpty()) {
            return new AdminDeleteResultDto(0, 0, 0);
        }

        int deletedCount = 0;
        for (Long boardNo : boardNoList) {
            deletedCount += deleteBoard(boardNo);
        }
        return new AdminDeleteResultDto(boardNoList.size(), deletedCount, boardNoList.size() - deletedCount);
    }



    private AdminBoardDto validateAnswerTarget(Long boardNo) {
        if (boardNo == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_ANSWER_TARGET_NOT_FOUND);
        }

        AdminBoardDto board = adminBoardDao.findByNo(boardNo);
        if (board == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_ANSWER_TARGET_NOT_FOUND);
        }

        if (CATEGORY_NOTICE.equalsIgnoreCase(board.getCategory())) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_ANSWER_NOTICE_DENIED);
        }

        return board;
    }

    private void prepareForSave(AdminBoardDto boardDto) {
        if (boardDto == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_SAVE_FAILED);
        }

        String title = clean(boardDto.getTitle());
        String content = clean(boardDto.getContent());

        if (title == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_SAVE_TITLE_REQUIRED);
        }
        if (content == null) {
            throw new IllegalArgumentException(AdminFlashMessage.BOARD_SAVE_CONTENT_REQUIRED);
        }

        boardDto.setTitle(title);
        boardDto.setContent(content);
        boardDto.setCategory(normalizeCategory(boardDto.getCategory()));
        boardDto.setAnswerStatus(normalizeAnswerStatus(boardDto.getAnswerStatus()));
    }

    private String normalizeCategory(String category) {
        if (CATEGORY_NOTICE.equalsIgnoreCase(category)) {
            return CATEGORY_NOTICE;
        }
        return CATEGORY_QUESTION;
    }

    private String normalizeAnswerStatus(String answerStatus) {
        if (ANSWER_ANSWERED.equalsIgnoreCase(answerStatus)) {
            return ANSWER_ANSWERED;
        }
        return ANSWER_WAITING;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleanValue = value.trim();
        return cleanValue.isEmpty() ? null : cleanValue;
    }
}