package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 마이페이지 최근 게시글 목록 DTO입니다. */
@Getter
@Setter
public class MyPageRecentBoardDto {

    private Integer no;
    private String category;
    private String answerStatus;
    private String title;
    private LocalDateTime createdDate;
}
