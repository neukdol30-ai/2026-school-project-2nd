package com.siyan1234.itproject2nd.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class ChatRoomDto {
    private Integer roomNo;
    private Integer userNo;
    private Integer adminNo;
    private String status;
    private String category;
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdDate;
    private LocalDateTime closedDate;

    public String getCategoryName(){
        if (category==null){
            return "일반 문의";
        }
        return switch (category){
            case "MAIL" -> "메일 문의";
            case "MAP" -> "지도 문의";
            case "STOCK" -> "증권 문의";
            case "NEWS" -> "뉴스 문의";
            case "WEATHER" -> "날씨 문의";
            case "CALENDAR" -> "캘린더 문의";
            case "ETC" -> "기타 문의";
            default -> "일반 문의";
        };
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
