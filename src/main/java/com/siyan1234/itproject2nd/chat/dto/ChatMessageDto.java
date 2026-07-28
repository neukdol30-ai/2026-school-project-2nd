package com.siyan1234.itproject2nd.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/** DB 저장, Redis 임시 저장, WebSocket 전송에서 공통으로 사용하는 채팅 메시지 DTO입니다. */
@Getter
@Setter
@ToString
public class ChatMessageDto {
    private Integer messageNo;
    private Integer roomNo;
    private Integer senderNo;
    private String messageContent;
    private String readYn;
    private LocalDateTime createdDate;
}
