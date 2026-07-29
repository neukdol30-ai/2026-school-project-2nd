package com.siyan1234.itproject2nd.admin.support;

/** 관리자 콘솔에서 공통으로 사용하는 페이징/검색어 보정 유틸입니다. */
public final class AdminPagingHelper {

    public static final int MAX_PAGE_SIZE = 50;

    private AdminPagingHelper() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 1);
    }

    public static int normalizeSize(int size, int defaultSize) {
        if (size < 1) {
            return defaultSize;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    public static int calculateOffset(int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = Math.max(size, 1);
        return (safePage - 1) * safeSize;
    }

    public static int calculateTotalPages(long totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }

        int safeSize = Math.max(size, 1);
        return (int) Math.ceil((double) totalCount / safeSize);
    }

    public static String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }

    asdffasdf
}
