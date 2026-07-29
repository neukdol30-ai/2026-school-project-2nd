package com.siyan1234.itproject2nd.calendar.service;

import com.siyan1234.itproject2nd.calendar.dao.CalendarDao;
import com.siyan1234.itproject2nd.calendar.dto.CalendarDayDto;
import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CalendarService {

    private final CalendarDao calendarDao;
    private final GoogleCalendarService googleCalendarService;

    // 월간 달력 생성
    public List<CalendarDayDto> makeMonthCalendar(
            LocalDate selectedDate
    ) {

        List<CalendarDayDto> calendarList =
                new ArrayList<>();

        LocalDate firstDayOfMonth =
                selectedDate.withDayOfMonth(1);

        LocalDate lastDayOfMonth =
                selectedDate.withDayOfMonth(
                        selectedDate.lengthOfMonth()
                );

        LocalDate startDate =
                firstDayOfMonth;

        // 달력 시작 날짜를 일요일까지 이동
        while (startDate.getDayOfWeek()
                != DayOfWeek.SUNDAY) {

            startDate =
                    startDate.minusDays(1);
        }

        LocalDate endDate =
                lastDayOfMonth;

        // 달력 마지막 날짜를 토요일까지 이동
        while (endDate.getDayOfWeek()
                != DayOfWeek.SATURDAY) {

            endDate =
                    endDate.plusDays(1);
        }

        LocalDate date =
                startDate;

        while (!date.isAfter(endDate)) {

            CalendarDayDto dayDto =
                    new CalendarDayDto();

            dayDto.setDate(date);

            calendarList.add(dayDto);

            date =
                    date.plusDays(1);
        }

        return calendarList;
    }

    // 선택 날짜 일정 조회
    public List<CalendarEventDto> findByDate(
            int memberNo,
            String date
    ) {

        return calendarDao.findByDate(
                memberNo,
                date
        );
    }

    // 예정 일정 조회
    // 메인 화면 일정 위젯에서 사용
    public List<CalendarEventDto> findTodayEvents(
            int memberNo
    ) {

        return calendarDao.findTodayEvents(
                memberNo
        );
    }


// 월별 일정 날짜 조회
// 역할: 현재 보고 있는 달에서 일정이 있는 날짜 목록 반환
    public List<String> findEventDatesByMonth(
            int memberNo,
            int year,
            int month
    ) {

        // 조회할 달의 첫째 날
        LocalDate monthStartDate =
                LocalDate.of(
                        year,
                        month,
                        1
                );

        // 다음 달 첫째 날
        LocalDate monthEndDate =
                monthStartDate.plusMonths(1);

        return calendarDao.findEventDatesByMonth(
                memberNo,
                monthStartDate.toString(),
                monthEndDate.toString()
        );
    }

    // 일정 등록
    // 사이트 DB와 구글 캘린더에 함께 등록
    @Transactional
    public int insert(
            CalendarEventDto calendarEventDto
    ) {

        int memberNo =
                calendarEventDto.getMemberNo();

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        if (!googleConnected) {
            throw new IllegalStateException(
                    "개인 일정을 등록하려면 구글 캘린더 연동이 필요합니다."
            );
        }

        String eventDate =
                calendarEventDto.getEventDate();

        if (eventDate == null
                || eventDate.isBlank()) {

            throw new IllegalArgumentException(
                    "일정 날짜가 없습니다."
            );
        }

        String startTime =
                calendarEventDto.getStartTime();

        String endTime =
                calendarEventDto.getEndTime();

        boolean hasStartTime =
                startTime != null
                        && !startTime.isBlank();

        boolean hasEndTime =
                endTime != null
                        && !endTime.isBlank();


        // 시작·종료 시간은 둘 다 입력하거나 둘 다 비워야 함
        if (hasStartTime != hasEndTime) {
            throw new IllegalArgumentException(
                    "시작 시간과 종료 시간을 모두 선택해주세요."
            );
        }


        if (hasStartTime) {

            startTime =
                    startTime.trim();

            endTime =
                    endTime.trim();

            try {
                LocalTime start =
                        LocalTime.parse(startTime);

                LocalTime end =
                        LocalTime.parse(endTime);

                if (!end.isAfter(start)) {
                    throw new IllegalArgumentException(
                            "종료 시간은 시작 시간보다 늦어야 합니다."
                    );
                }

            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(
                        "시간 형식이 올바르지 않습니다."
                );
            }

            // 시작·종료 시간이 있는 일정
            calendarEventDto.setAllDayYn("N");

            calendarEventDto.setStartTime(
                    startTime
            );

            calendarEventDto.setEndTime(
                    endTime
            );

            calendarEventDto.setStartDatetime(
                    eventDate
                            + " "
                            + startTime
            );

            calendarEventDto.setEndDatetime(
                    eventDate
                            + " "
                            + endTime
            );

        } else {

            // 시간을 선택하지 않은 날짜 일정
            calendarEventDto.setAllDayYn("Y");

            calendarEventDto.setStartTime(null);
            calendarEventDto.setEndTime(null);

            calendarEventDto.setStartDatetime(
                    eventDate + " 00:00"
            );

            calendarEventDto.setEndDatetime(
                    eventDate + " 23:59"
            );
        }

        calendarEventDto.setSourceType("LOCAL");
        calendarEventDto.setGoogleCalendarId("primary");
        calendarEventDto.setIsDeleted("N");


        // 구글 캘린더에 먼저 일정 생성
        String googleEventId =
                googleCalendarService.createGoogleEvent(
                        memberNo,
                        calendarEventDto
                );

        if (googleEventId == null
                || googleEventId.isBlank()) {

            throw new IllegalStateException(
                    "구글 캘린더 일정 등록에 실패했습니다."
            );
        }

        calendarEventDto.setGoogleEventId(
                googleEventId
        );


        // 구글 일정 ID와 함께 DB에 저장
        return calendarDao.insert(
                calendarEventDto
        );
    }

    // 일정 수정
    // 구글 캘린더와 사이트 DB를 함께 수정
    @Transactional
    public int update(
            CalendarEventDto calendarEventDto
    ) {

        int memberNo =
                calendarEventDto.getMemberNo();

        int eventNo =
                calendarEventDto.getNo();


        // 구글 캘린더 연동 여부 확인
        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        if (!googleConnected) {
            throw new IllegalStateException(
                    "개인 일정을 수정하려면 구글 캘린더 연동이 필요합니다."
            );
        }


        // 수정할 일정 번호 검사
        if (eventNo <= 0) {
            throw new IllegalArgumentException(
                    "수정할 일정 번호가 올바르지 않습니다."
            );
        }


    /*
        일정 번호와 로그인 회원 번호로 조회한다.

        다른 회원의 일정 번호를 임의로 전달해도
        조회되지 않으므로 수정할 수 없다.
    */
        CalendarEventDto oldEvent =
                calendarDao.findByNo(
                        eventNo,
                        memberNo
                );

        if (oldEvent == null) {
            throw new IllegalArgumentException(
                    "수정할 일정을 찾을 수 없습니다."
            );
        }


        // 일정 날짜 검사
        String eventDate =
                calendarEventDto.getEventDate();

        if (
                eventDate == null
                        || eventDate.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "일정 날짜가 없습니다."
            );
        }


        // 일정 제목 검사
        String title =
                calendarEventDto.getTitle();

        if (
                title == null
                        || title.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "일정 제목을 입력해주세요."
            );
        }

        calendarEventDto.setTitle(
                title.trim()
        );


        // 내용과 장소의 앞뒤 공백 정리
        String content =
                calendarEventDto.getContent();

        String location =
                calendarEventDto.getLocation();

        calendarEventDto.setContent(
                content == null
                        || content.isBlank()
                        ? null
                        : content.trim()
        );

        calendarEventDto.setLocation(
                location == null
                        || location.isBlank()
                        ? null
                        : location.trim()
        );


        String startTime =
                calendarEventDto.getStartTime();

        String endTime =
                calendarEventDto.getEndTime();

        boolean hasStartTime =
                startTime != null
                        && !startTime.isBlank();

        boolean hasEndTime =
                endTime != null
                        && !endTime.isBlank();


        // 시작 시간과 종료 시간은 둘 다 입력하거나 둘 다 비워야 함
        if (hasStartTime != hasEndTime) {
            throw new IllegalArgumentException(
                    "시작 시간과 종료 시간을 모두 선택해주세요."
            );
        }


        if (hasStartTime) {

            startTime =
                    startTime.trim();

            endTime =
                    endTime.trim();

            try {

                LocalTime start =
                        LocalTime.parse(
                                startTime
                        );

                LocalTime end =
                        LocalTime.parse(
                                endTime
                        );

                if (!end.isAfter(start)) {
                    throw new IllegalArgumentException(
                            "종료 시간은 시작 시간보다 늦어야 합니다."
                    );
                }

            } catch (DateTimeParseException e) {

                throw new IllegalArgumentException(
                        "시간 형식이 올바르지 않습니다."
                );
            }


            // 시간이 있는 일정
            calendarEventDto.setAllDayYn(
                    "N"
            );

            calendarEventDto.setStartTime(
                    startTime
            );

            calendarEventDto.setEndTime(
                    endTime
            );

            calendarEventDto.setStartDatetime(
                    eventDate
                            + " "
                            + startTime
            );

            calendarEventDto.setEndDatetime(
                    eventDate
                            + " "
                            + endTime
            );

        } else {

            // 시간을 선택하지 않은 날짜 일정
            calendarEventDto.setAllDayYn(
                    "Y"
            );

            calendarEventDto.setStartTime(
                    null
            );

            calendarEventDto.setEndTime(
                    null
            );

            calendarEventDto.setStartDatetime(
                    eventDate
                            + " 00:00"
            );

            calendarEventDto.setEndDatetime(
                    eventDate
                            + " 23:59"
            );
        }


    /*
        기존 일정의 구글 ID를 새 DTO에 다시 넣는다.

        수정 화면에서는 구글 일정 ID를 직접 받지 않으므로
        DB에서 조회한 기존 값을 사용해야 한다.
    */
        calendarEventDto.setGoogleEventId(
                oldEvent.getGoogleEventId()
        );

        calendarEventDto.setGoogleCalendarId(
                oldEvent.getGoogleCalendarId()
        );

        calendarEventDto.setSourceType(
                oldEvent.getSourceType()
        );

        calendarEventDto.setIsDeleted(
                "N"
        );


        // 연결된 구글 일정도 먼저 수정
        if (
                oldEvent.getGoogleEventId() != null
                        && !oldEvent
                        .getGoogleEventId()
                        .isBlank()
        ) {

            googleCalendarService
                    .updateGoogleEvent(
                            memberNo,
                            calendarEventDto
                    );
        }


        // 우리 사이트 DB 일정 수정
        int result =
                calendarDao.updateEvent(
                        calendarEventDto
                );

        if (result == 0) {
            throw new IllegalStateException(
                    "일정 수정에 실패했습니다."
            );
        }

        return result;
    }

    // 일정 삭제
    // 구글 캘린더와 사이트 DB에서 삭제
    @Transactional
    public int delete(
            int no,
            int memberNo
    ) {

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        if (!googleConnected) {
            throw new IllegalStateException(
                    "개인 일정을 삭제하려면 구글 캘린더 연동이 필요합니다."
            );
        }

        CalendarEventDto oldEvent =
                calendarDao.findByNo(
                        no,
                        memberNo
                );

        if (oldEvent == null) {

            System.out.println(
                    "[캘린더 삭제] 삭제할 일정 없음"
                            + " no = " + no
                            + ", memberNo = " + memberNo
            );

            return 0;
        }

        // 구글 일정 ID가 있으면 구글에서도 삭제
        if (oldEvent.getGoogleEventId() != null
                && !oldEvent.getGoogleEventId().isBlank()) {

            try {
                googleCalendarService.deleteGoogleEvent(
                        memberNo,
                        oldEvent.getGoogleEventId()
                );

                System.out.println(
                        "[캘린더 삭제] 구글 일정 삭제 완료"
                                + " googleEventId = "
                                + oldEvent.getGoogleEventId()
                );

            } catch (Exception e) {

                System.out.println(
                        "[캘린더 삭제] 구글 일정 삭제 실패"
                );

                System.out.println(
                        "googleEventId = "
                                + oldEvent.getGoogleEventId()
                );

                System.out.println(
                        "error = "
                                + e.getMessage()
                );
            }
        }

        // DB에서는 IS_DELETED를 Y로 변경
        int result =
                calendarDao.delete(
                        no,
                        memberNo
                );

        System.out.println(
                "[캘린더 삭제] DB 삭제 결과 result = "
                        + result
        );

        return result;
    }


    // 월간 달력에 일정 제목과 공휴일 정보를 함께 넣는다.
    public List<CalendarDayDto> makeMonthCalendarWithEvents(
            LocalDate selectedDate,
            int memberNo,
            boolean googleConnected
    ) {

        List<CalendarDayDto> calendarList =
                makeMonthCalendar(
                        selectedDate
                );

        Map<String, List<CalendarEventDto>> eventMap =
                new HashMap<>();

        if (googleConnected) {

            LocalDate monthStartDate =
                    selectedDate.withDayOfMonth(1);

            LocalDate monthEndDate =
                    monthStartDate.plusMonths(1);

            List<CalendarEventDto> monthEventList =
                    calendarDao.findMonthEvents(
                            memberNo,
                            monthStartDate.toString(),
                            monthEndDate.toString()
                    );

            if (monthEventList != null) {

                for (CalendarEventDto eventItem
                        : monthEventList) {

                    if (eventItem == null
                            || eventItem.getEventDate() == null
                            || eventItem.getEventDate().isBlank()) {

                        continue;
                    }

                    eventMap
                            .computeIfAbsent(
                                    eventItem.getEventDate(),
                                    key -> new ArrayList<>()
                            )
                            .add(eventItem);
                }
            }
        }

        LocalDate calendarStartDate =
                calendarList
                        .get(0)
                        .getDate();

        LocalDate calendarEndDate =
                calendarList
                        .get(calendarList.size() - 1)
                        .getDate();

        List<CalendarEventDto> holidayList =
                googleCalendarService
                        .getKoreaHolidayEvents(
                                memberNo,
                                calendarStartDate,
                                calendarEndDate
                        );

        Map<String, String> holidayMap =
                new HashMap<>();

        for (CalendarEventDto holiday
                : holidayList) {

            String holidayDate =
                    holiday.getEventDate();

            String holidayName =
                    normalizeHolidayName(
                            holiday.getTitle()
                    );

            if (holidayDate == null
                    || holidayName == null
                    || holidayName.isBlank()) {

                continue;
            }

            holidayMap.merge(
                    holidayDate,
                    holidayName,
                    (oldName, newName) ->
                            oldName
                                    + ", "
                                    + newName
            );
        }

        for (CalendarDayDto day
                : calendarList) {

            String dateText =
                    day.getDate().toString();

            List<CalendarEventDto> dayEventList =
                    eventMap.getOrDefault(
                            dateText,
                            List.of()
                    );

            day.setHasEvent(
                    !dayEventList.isEmpty()
            );

            day.setEventCount(
                    dayEventList.size()
            );

            if (dayEventList.isEmpty()) {

                day.setEventTitle(
                        null
                );

            } else {

                String eventTitle =
                        dayEventList
                                .get(0)
                                .getTitle();

                day.setEventTitle(
                        eventTitle == null
                                || eventTitle.isBlank()
                                ? "제목 없음"
                                : eventTitle.trim()
                );
            }

            String holidayName =
                    holidayMap.get(
                            dateText
                    );

            day.setHoliday(
                    holidayName != null
                            && !holidayName.isBlank()
            );

            day.setHolidayName(
                    holidayName
            );
        }

        return calendarList;
    }

    // 공휴일 이름 정리
    private String normalizeHolidayName(
            String holidayName
    ) {

        if (holidayName == null) {
            return null;
        }

        String normalizedHolidayName =
                holidayName
                        .replaceAll(
                                "[\\s\\u00A0]+",
                                " "
                        )
                        .trim();

        String compactHolidayName =
                normalizedHolidayName
                        .replace(" ", "");

        if (compactHolidayName.contains("쉬는날")) {
            return "대체공휴일";
        }

        return normalizedHolidayName;
    }
}