package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

/** 최근 N일 상담 접수 추이 DTO입니다. */
@Getter
@Setter
public class DailyChatStatsDto {

    private String dateLabel;
    private Long chatCount;
}
