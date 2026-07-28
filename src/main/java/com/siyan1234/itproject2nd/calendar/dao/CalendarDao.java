package com.siyan1234.itproject2nd.calendar.dao;

import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CalendarDao {

    // =========================
    // 일정 조회
    // =========================

    // 선택한 날짜의 일정 목록 조회
    List<CalendarEventDto> findByDate(
            @Param("memberNo") int memberNo,
            @Param("date") String date
    );


    // 로그인 회원의 오늘 이후 예정 일정 조회
    // 메인 화면의 일정 위젯에서 사용
    List<CalendarEventDto> findTodayEvents(
            @Param("memberNo") int memberNo
    );


    // 일정 번호와 회원 번호로 일정 한 건 조회
    CalendarEventDto findByNo(
            @Param("no") int no,
            @Param("memberNo") int memberNo
    );


    // 선택한 월에서 일정이 존재하는 날짜 목록 조회
    List<String> findEventDatesByMonth(
            @Param("memberNo") int memberNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );


    // =========================
    // 우리 사이트 일정
    // =========================

    // 우리 사이트 일정 등록
    int insert(
            CalendarEventDto calendarEventDto
    );


    /*
        로그인 회원 본인의 일정 수정

        일정 번호와 회원 번호를 함께 검사해서
        다른 회원의 일정은 수정할 수 없게 한다.
    */
    int updateEvent(
            CalendarEventDto calendarEventDto
    );


    // 로그인 회원 본인의 일정 삭제 처리
    int delete(
            @Param("no") int no,
            @Param("memberNo") int memberNo
    );


    // =========================
    // 구글 일정 동기화
    // =========================

    // 구글 일정 ID로 기존 일정 조회
    CalendarEventDto findByGoogleEventId(
            @Param("memberNo") int memberNo,
            @Param("googleEventId") String googleEventId
    );


    // 구글에서 가져온 새 일정 저장
    int insertGoogleEvent(
            CalendarEventDto calendarEventDto
    );


    // 이미 저장된 구글 일정 수정
    int updateGoogleEvent(
            CalendarEventDto calendarEventDto
    );


    // 구글에서 사라진 일정 삭제 처리
    int deleteMissingGoogleEvents(
            @Param("memberNo") int memberNo,
            @Param("googleEventIdList") List<String> googleEventIdList,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );

    List<CalendarEventDto> findMonthEvents(
            @Param("memberNo") int memberNo,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate
    );
}