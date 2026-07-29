package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** 관리자 방문 기록 화면의 장기·당일 집계 DTO입니다. */
@Getter
@Setter
public class AdminVisitOverviewDto {

    /** 오늘 방문한 고유 IP 수입니다. */
    private Long todayVisitorCount;

    /** 날짜별 고유 방문자 수를 누적한 합계입니다. */
    private Long totalDailyVisitorCount;

    /** 방문 기록 대상 요청의 누적 횟수입니다. */
    private Long totalRequestCount;

    private Long memberVisitorCount;
    private Long guestVisitorCount;
    private LocalDate firstStatDate;
    private LocalDate lastStatDate;

    /** 화면 안내용 원본 로그 보관 일수입니다. */
    private Integer retentionDays;
}
