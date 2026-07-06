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
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdDate;
    private LocalDateTime closedDate;
}
