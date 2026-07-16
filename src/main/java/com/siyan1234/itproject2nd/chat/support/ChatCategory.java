package com.siyan1234.itproject2nd.chat.support;

import java.util.Map;
import java.util.Set;

/**
 * 상담 문의 유형 문자열을 한 곳에서 관리하는 클래스입니다.
 *
 * DB의 chat_room.category CHECK 제약조건과 화면 표시 이름/아이콘이
 * 서로 달라지지 않도록 category 관련 값을 이 클래스에서 관리합니다.
 */
public final class ChatCategory {

    public static final String MAIL = "MAIL";
    public static final String MAP = "MAP";
    public static final String STOCK = "STOCK";
    public static final String NEWS = "NEWS";
    public static final String WEATHER = "WEATHER";
    public static final String CALENDAR = "CALENDAR";
    public static final String ETC = "ETC";

    public static final String DEFAULT = ETC;

    private static final Set<String> ALLOWED = Set.of(
            MAIL,
            MAP,
            STOCK,
            NEWS,
            WEATHER,
            CALENDAR,
            ETC
    );

    private static final Map<String, String> NAME_MAP = Map.of(
            MAIL, "메일 문의",
            MAP, "지도 문의",
            STOCK, "증권 문의",
            NEWS, "뉴스 문의",
            WEATHER, "날씨 문의",
            CALENDAR, "캘린더 문의",
            ETC, "기타 문의"
    );

    private static final Map<String, String> ICON_MAP = Map.of(
            MAIL, "📧",
            MAP, "🗺",
            STOCK, "📈",
            NEWS, "📰",
            WEATHER, "🌤",
            CALENDAR, "📅",
            ETC, "💬"
    );

    private ChatCategory() {
    }

    public static String normalize(String category) {
        if (category == null || category.isBlank()) {
            return DEFAULT;
        }

        String cleanCategory = category.trim().toUpperCase();
        return isValid(cleanCategory) ? cleanCategory : DEFAULT;
    }

    public static boolean isValid(String category) {
        return category != null && ALLOWED.contains(category);
    }

    public static String displayName(String category) {
        return NAME_MAP.getOrDefault(category, "일반 문의");
    }

    public static String icon(String category) {
        return ICON_MAP.getOrDefault(category, "💬");
    }
}
