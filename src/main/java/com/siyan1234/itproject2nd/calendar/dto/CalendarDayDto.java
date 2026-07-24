package com.siyan1234.itproject2nd.calendar.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CalendarDayDto {

    // 실제 날짜
    private LocalDate date;

    // 해당 날짜에 일정이 있는지 확인
    private boolean hasEvent;

    // 해당 날짜가 대한민국 휴일인지 확인
    private boolean holiday;

    // 해당 날짜의 휴일 이름
    // 예: 제헌절, 광복절, 추석
    private String holidayName;
}