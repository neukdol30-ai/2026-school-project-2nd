package com.siyan1234.itproject2nd.chat.dto;

import com.siyan1234.itproject2nd.chat.support.ChatCategory;
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

    //관리자 목록 안읽은 메시지 개수
    private Integer unreadCount;

    public String getCategoryName() {
        return ChatCategory.displayName(category);
    }

    public String getCategoryIcon() {
        return ChatCategory.icon(category);
    }
}
