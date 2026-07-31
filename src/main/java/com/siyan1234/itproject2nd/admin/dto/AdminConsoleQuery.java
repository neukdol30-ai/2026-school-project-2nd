package com.siyan1234.itproject2nd.admin.dto;

import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import lombok.Getter;
import lombok.Setter;

/**
 * 관리자 단일 콘솔 화면의 검색·페이징·상세 조회 파라미터를 묶는 요청 DTO입니다.
 *
 * <p>관리자 목록의 페이지 크기는 서버에서 10건으로 고정합니다. 따라서
 * {@code memberSize}, {@code chatSize}, {@code boardSize}, {@code visitSize} 같은
 * 요청 파라미터는 DTO에 두지 않으며 주소창으로 값을 조작해도 조회 건수에 영향을 줄 수 없습니다.</p>
 */
@Getter
@Setter
public class AdminConsoleQuery {

    private String view = "dashboard";

    private String memberKeyword;
    private int memberPage = 1;
    private Integer editMemberNo;

    private String chatStatus;
    private String chatCategory;
    private String chatKeyword;
    private int chatPage = 1;
    private Integer roomNo;

    private String boardCategory;
    private String boardKeyword;
    private int boardPage = 1;
    private Long boardNo;

    private String visitKeyword;
    private int visitPage = 1;

    /** Spring 바인딩이 끝난 뒤 검색어와 페이지 번호를 안전한 범위로 보정합니다. */
    public void normalize() {
        view = normalizeView(view);

        memberKeyword = AdminPagingHelper.cleanText(memberKeyword);
        memberPage = AdminPagingHelper.normalizePage(memberPage);

        chatStatus = AdminPagingHelper.cleanText(chatStatus);
        chatCategory = AdminPagingHelper.cleanText(chatCategory);
        chatKeyword = AdminPagingHelper.cleanText(chatKeyword);
        chatPage = AdminPagingHelper.normalizePage(chatPage);

        boardCategory = AdminPagingHelper.cleanText(boardCategory);
        boardKeyword = AdminPagingHelper.cleanText(boardKeyword);
        boardPage = AdminPagingHelper.normalizePage(boardPage);

        visitKeyword = AdminPagingHelper.cleanText(visitKeyword);
        visitPage = AdminPagingHelper.normalizePage(visitPage);
    }

    private String normalizeView(String value) {
        if (value == null || value.isBlank()) {
            return "dashboard";
        }
        return value.trim();
    }
}
