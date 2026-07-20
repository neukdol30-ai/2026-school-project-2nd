package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MyPageRecentBoardDto {

    private Integer no;
    private String category;
    private String answerStatus;
    private String title;
    private LocalDateTime createdDate;
}
