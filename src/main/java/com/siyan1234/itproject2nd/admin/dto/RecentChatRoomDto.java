package com.siyan1234.itproject2nd.admin.dto;

import com.siyan1234.itproject2nd.chat.support.ChatCategory;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
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
        return ChatRoomStatus.displayName(status);
    }

    public String getCategoryIcon() {
        return ChatCategory.icon(category);
    }
}
