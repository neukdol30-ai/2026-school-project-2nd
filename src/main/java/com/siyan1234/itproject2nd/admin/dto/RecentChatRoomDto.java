package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 관리자 대시보드의 최근 상담 목록 DTO입니다. */
@Getter
@Setter
public class RecentChatRoomDto {

    private Integer roomNo;
    private Integer userNo;
    private Integer adminNo;
    private String userNickname;
    private String adminNickname;
    private String status;
    private String category;
    private String categoryName;
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdDate;
    private Long unreadCount;

    public String getStatusName() {
        if ("OPEN".equals(status)) {
            return "진행 중";
        }

        if ("CLOSED".equals(status)) {
            return "종료";
        }

        return "확인 필요";
    }

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
