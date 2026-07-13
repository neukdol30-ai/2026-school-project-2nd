package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 상담 카테고리별 상담방 개수 통계 DTO입니다. */
@Getter
@Setter
public class CategoryStatsDto {

    private String category;
    private String categoryName;
    private Long chatCount;
    private Long openCount;
    private Long closedCount;

    public String getCategoryIcon() {
        if (category == null) {
            return "💬";
        }

        return switch (category) {
            case "MAIL" -> "📧";
            case "MAP" -> "🗺";
            case "STOCK" -> "📈";
            case "NEWS" -> "📰";
            case "WEATHER" -> "🌤";
            case "CALENDAR" -> "📅";
            case "ETC" -> "💬";
            default -> "💬";
        };
    }
}
