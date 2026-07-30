package com.siyan1234.itproject2nd.admin.support;

import java.util.ArrayList;
import java.util.List;

/** 관리자 콘솔에서 공통으로 사용하는 페이징/검색어 보정 유틸입니다. */
public final class AdminPagingHelper {

    /** 회원·상담·게시글·방문 기록 목록에 공통 적용되는 고정 페이지 크기입니다. */
    public static final int PAGE_SIZE = 10;
    private static final int DEFAULT_PAGE_WINDOW_SIZE = 5;

    private AdminPagingHelper() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    /**
     * 전체 건수를 기준으로 요청 페이지를 실제 존재하는 마지막 페이지 안으로 보정합니다.
     * 검색 결과가 0건이어도 화면상 1페이지를 유지합니다.
     */
    public static int clampPage(int page, long totalCount) {
        int totalPages = calculateTotalPages(totalCount);
        return Math.min(normalizePage(page), totalPages);
    }

    public static int calculateOffset(int page) {
        int safePage = normalizePage(page);
        long offset = (long) (safePage - 1) * PAGE_SIZE;
        return offset > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) offset;
    }

    public static int calculateTotalPages(long totalCount) {
        if (totalCount <= 0) {
            return 1;
        }

        // 덧셈 오버플로를 피하기 위해 ceil(totalCount / PAGE_SIZE)를 아래 형태로 계산합니다.
        long totalPages = 1L + ((totalCount - 1L) / PAGE_SIZE);
        return totalPages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) totalPages;
    }

    /**
     * 페이지가 매우 많아도 화면에 모든 번호를 렌더링하지 않고 현재 페이지 주변만 반환합니다.
     */
    public static List<Integer> buildPageNumbers(int currentPage, int totalPages) {
        return buildPageNumbers(currentPage, totalPages, DEFAULT_PAGE_WINDOW_SIZE);
    }

    public static List<Integer> buildPageNumbers(int currentPage, int totalPages, int windowSize) {
        int safeTotalPages = Math.max(totalPages, 1);
        int safeCurrentPage = Math.min(normalizePage(currentPage), safeTotalPages);
        int safeWindowSize = Math.max(windowSize, 1);
        int halfWindow = safeWindowSize / 2;

        int startPage = Math.max(1, safeCurrentPage - halfWindow);
        int endPage = Math.min(safeTotalPages, startPage + safeWindowSize - 1);
        startPage = Math.max(1, endPage - safeWindowSize + 1);

        List<Integer> pageNumbers = new ArrayList<>();
        for (int pageNo = startPage; pageNo <= endPage; pageNo++) {
            pageNumbers.add(pageNo);
        }
        return pageNumbers;
    }

    public static String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
