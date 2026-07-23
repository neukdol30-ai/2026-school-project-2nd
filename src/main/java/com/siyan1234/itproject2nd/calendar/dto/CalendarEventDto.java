package com.siyan1234.itproject2nd.calendar.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CalendarEventDto {

    // 일정 기본 정보
    private int no;
    private int memberNo;
    private String title;
    private String content;
    private String location;

    // 화면 입력 날짜 / 시간
    private String eventDate;
    private String startTime;
    private String endTime;

    // 실제 저장 날짜 / 시간
    private String startDatetime;
    private String endDatetime;
    private String allDayYn;

    // 구글 캘린더 연동 정보
    private String sourceType;
    private String googleEventId;
    private String googleCalendarId;
    private String googleUpdatedDatetime;

    // 삭제 여부
    private String isDeleted;
}