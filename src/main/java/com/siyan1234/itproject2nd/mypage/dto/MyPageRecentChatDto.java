package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 마이페이지 최근 1:1 상담 목록 DTO입니다. */
@Getter
@Setter
public class MyPageRecentChatDto {

    private Integer roomNo;
    private String category;
    private String status;
    private String lastMessage;
    private LocalDateTime lastMessageDate;
    private LocalDateTime createdDate;
}
