package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminBoardDao;
import com.siyan1234.itproject2nd.admin.dto.AdminBoardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
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
