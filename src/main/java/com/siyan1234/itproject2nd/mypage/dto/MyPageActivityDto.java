package com.siyan1234.itproject2nd.mypage.dto;

import lombok.Getter;
import lombok.Setter;

/** 마이페이지 내 활동 요약 카드 DTO입니다. */
@Getter
@Setter
public class MyPageActivityDto {

    private int boardCount;
    private int waitingQuestionCount;
    private int chatCount;
    private int openChatCount;
}
