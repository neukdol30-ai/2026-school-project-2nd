package com.siyan1234.itproject2nd.chat.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

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