package com.siyan1234.itproject2nd.admin.dto;

import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 단일 콘솔 화면의 검색·페이징·상세 조회 파라미터를 묶는 요청 DTO입니다.
 *
 * <p>Controller 메서드에 다수의 {@code @RequestParam}을 나열하지 않고 한 객체로 바인딩하며,
 * 화면 조회 전에 페이지 번호·페이지 크기·검색어를 동일한 기준으로 정규화합니다.</p>
 */
@Getter
@Setter
public class AdminConsoleQuery {

    private static final int DEFAULT_MEMBER_SIZE = 10;
    private static final int DEFAULT_CHAT_SIZE = 10;
    private static final int DEFAULT_BOARD_SIZE = 10;
    private static final int DEFAULT_VISIT_SIZE = 10;

    private String view = "dashboard";

    private String memberKeyword;
    private int memberPage = 1;
    private int memberSize = DEFAULT_MEMBER_SIZE;
    private Integer editMemberNo;

    private String chatStatus;
    private String chatCategory;
    private String chatKeyword;
    private int chatPage = 1;
    private int chatSize = DEFAULT_CHAT_SIZE;
    private Integer roomNo;

    private String boardCategory;
    private String boardKeyword;
    private int boardPage = 1;
    private int boardSize = DEFAULT_BOARD_SIZE;
    private Long boardNo;

    private String visitKeyword;
    private int visitPage = 1;
    private int visitSize = DEFAULT_VISIT_SIZE;

    /** Spring 바인딩이 끝난 뒤 검색어와 페이징 값을 안전한 범위로 보정합니다. */
    public void normalize() {
        view = normalizeView(view);

        memberKeyword = AdminPagingHelper.cleanText(memberKeyword);
        memberPage = AdminPagingHelper.normalizePage(memberPage);
        memberSize = AdminPagingHelper.normalizeSize(memberSize, DEFAULT_MEMBER_SIZE);

        chatStatus = AdminPagingHelper.cleanText(chatStatus);
        chatCategory = AdminPagingHelper.cleanText(chatCategory);
        chatKeyword = AdminPagingHelper.cleanText(chatKeyword);
        chatPage = AdminPagingHelper.normalizePage(chatPage);
        chatSize = AdminPagingHelper.normalizeSize(chatSize, DEFAULT_CHAT_SIZE);

        boardCategory = AdminPagingHelper.cleanText(boardCategory);
        boardKeyword = AdminPagingHelper.cleanText(boardKeyword);
        boardPage = AdminPagingHelper.normalizePage(boardPage);
        boardSize = AdminPagingHelper.normalizeSize(boardSize, DEFAULT_BOARD_SIZE);

        visitKeyword = AdminPagingHelper.cleanText(visitKeyword);
        visitPage = AdminPagingHelper.normalizePage(visitPage);
        visitSize = AdminPagingHelper.normalizeSize(visitSize, DEFAULT_VISIT_SIZE);
    }

    private String normalizeView(String value) {
        if (value == null || value.isBlank()) {
            return "dashboard";
        }
        return value.trim();
    }
}
