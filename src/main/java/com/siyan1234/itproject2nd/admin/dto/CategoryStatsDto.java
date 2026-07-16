package com.siyan1234.itproject2nd.admin.dto;

import com.siyan1234.itproject2nd.chat.support.ChatCategory;
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
        return ChatCategory.icon(category);
    }
}
