package com.siyan1234.itproject2nd.chat.dto;

import com.siyan1234.itproject2nd.chat.support.ChatCategory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/** 사용자와 담당 관리자, 상담 상태, 마지막 메시지 정보를 표현하는 상담방 DTO입니다. */
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

    /** 관리자 상담 목록에서 사용하는 미읽음 메시지 수입니다. */
    private Integer unreadCount;

    public String getCategoryName() {
        return ChatCategory.displayName(category);
    }

    public String getCategoryIcon() {
        return ChatCategory.icon(category);
    }
}
