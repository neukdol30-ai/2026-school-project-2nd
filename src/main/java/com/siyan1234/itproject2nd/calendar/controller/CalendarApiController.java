package com.siyan1234.itproject2nd.calendar.controller;

import com.siyan1234.itproject2nd.calendar.dto.CalendarDayDto;
import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import com.siyan1234.itproject2nd.calendar.service.CalendarService;
import com.siyan1234.itproject2nd.calendar.service.GoogleCalendarService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class CalendarApiController {

    private final CalendarService calendarService;
    private final GoogleCalendarService googleCalendarService;


    // =========================
    // 선택 날짜 일정 조회
    // =========================
    @GetMapping("/events")
    public ResponseEntity<Map<String, Object>> findEvents(
            @RequestParam String date,
            HttpSession session
    ) {

        Integer memberNo =
                getMemberNo(session);

        ResponseEntity<Map<String, Object>> checkResult =
                checkRequest(
                        memberNo,
                        date
                );

        if (checkResult != null) {
            return checkResult;
        }

        List<CalendarEventDto> eventList =
                calendarService.findByDate(
                        memberNo,
                        date
                );

        return success(
                "일정 목록을 불러왔습니다.",
                eventList
        );
    }


    // =========================
    // 메인 미니 캘린더 월별 정보 조회
    // 일정 점, 공휴일, 구글 연동 상태를 함께 반환
    // 주소: /api/calendar/month-summary?year=2026&month=7
    // =========================
    @GetMapping("/month-summary")
    public ResponseEntity<Map<String, Object>> findMonthSummary(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session
    ) {

        if (
                year < 1
                        || month < 1
                        || month > 12
        ) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", false);
            response.put(
                    "message",
                    "조회할 연도와 월이 올바르지 않습니다."
            );
            response.put(
                    "calendarList",
                    Collections.emptyList()
            );
            response.put(
                    "googleConnected",
                    false
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        Integer memberNo =
                getMemberNo(session);


        /*
            비로그인 상태에서는 개인 일정 점은 표시하지 않고,
            공개 API Key로 조회한 공휴일만 표시한다.
        */
        if (memberNo == null) {

            List<CalendarDayDto> calendarList =
                    calendarService
                            .makeMonthCalendarWithEvents(
                                    LocalDate.of(
                                            year,
                                            month,
                                            1
                                    ),
                                    0,
                                    false
                            );

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put("success", true);
            response.put(
                    "message",
                    "미니 캘린더를 불러왔습니다."
            );
            response.put(
                    "calendarList",
                    calendarList
            );
            response.put(
                    "googleConnected",
                    false
            );

            response.put(
                    "loggedIn",
                    false
            );

            response.put(
                    "memberId",
                    ""
            );

            response.put(
                    "nickname",
                    ""
            );

            return ResponseEntity.ok(response);
        }


        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );


        /*
            큰 캘린더와 같은 서비스 메서드를 사용한다.

            - 달력 날짜
            - 일정 존재 여부
            - 공휴일 여부와 이름
        */
        List<CalendarDayDto> calendarList =
                calendarService
                        .makeMonthCalendarWithEvents(
                                LocalDate.of(
                                        year,
                                        month,
                                        1
                                ),
                                memberNo,
                                googleConnected
                        );


        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "미니 캘린더 정보를 불러왔습니다."
        );
        response.put(
                "calendarList",
                calendarList
        );
        response.put(
                "googleConnected",
                googleConnected
        );


        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        CustomUserDetails loginUser =
                authentication != null
                        && authentication.isAuthenticated()
                        && !(
                        authentication
                                instanceof AnonymousAuthenticationToken
                )
                        && authentication.getPrincipal()
                        instanceof CustomUserDetails customUserDetails

                        ? customUserDetails
                        : null;

        MemberDto loginMember =
                loginUser == null
                        ? null
                        : loginUser.getMemberDto();


        response.put(
                "loggedIn",
                loginMember != null
        );

        response.put(
                "memberId",
                loginMember == null
                        ? ""
                        : loginMember.getMemberId()
        );

        response.put(
                "nickname",
                loginMember == null
                        ? ""
                        : (
                        loginMember.getNickname() == null
                                || loginMember.getNickname().isBlank()

                        ? loginMember.getMemberId()
                        : loginMember.getNickname()
                )
        );

        return ResponseEntity.ok(response);
    }


    // =========================
    // 월별 일정 날짜 조회
    // 달력에 일정 점을 표시할 날짜 목록 반환
    // 주소: /api/calendar/event-dates?year=2026&month=7
    // =========================
    @GetMapping("/event-dates")
    public ResponseEntity<Map<String, Object>> findEventDatesByMonth(
            @RequestParam int year,
            @RequestParam int month,
            HttpSession session
    ) {

        // 로그인 회원 번호 조회
        Integer memberNo =
                getMemberNo(session);

        // 로그인하지 않은 경우
        if (memberNo == null) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "로그인이 필요합니다."
            );

            response.put(
                    "eventDateList",
                    Collections.emptyList()
            );

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(response);
        }


        // 연도와 월 검사
        if (
                year < 1
                        || month < 1
                        || month > 12
        ) {

            Map<String, Object> response =
                    new LinkedHashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    "조회할 연도와 월이 올바르지 않습니다."
            );

            response.put(
                    "eventDateList",
                    Collections.emptyList()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }


        // 해당 월에서 일정이 있는 날짜 목록 조회
        List<String> eventDateList =
                calendarService.findEventDatesByMonth(
                        memberNo,
                        year,
                        month
                );


        // 정상 응답 생성
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                "월별 일정 날짜를 불러왔습니다."
        );

        response.put(
                "eventDateList",
                eventDateList
        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================
    // 일정 등록
    // =========================
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> insertEvent(
            @RequestParam String eventDate,
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session
    ) {

        Integer memberNo =
                getMemberNo(session);

        ResponseEntity<Map<String, Object>> checkResult =
                checkRequest(
                        memberNo,
                        eventDate
                );

        if (checkResult != null) {
            return checkResult;
        }

        if (
                title == null
                        || title.isBlank()
        ) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "일정 제목을 입력해주세요."
            );
        }

        String checkedStartTime =
                trimToNull(startTime);

        String checkedEndTime =
                trimToNull(endTime);


        /*
            시작 시간과 종료 시간은
            둘 다 입력하거나 둘 다 비워야 한다.
        */
        if (
                (checkedStartTime == null)
                        != (checkedEndTime == null)
        ) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "시작 시간과 종료 시간을 모두 선택해주세요."
            );
        }


        /*
            시간이 입력됐다면
            시간 형식과 순서를 검사한다.
        */
        if (checkedStartTime != null) {

            try {

                LocalTime start =
                        LocalTime.parse(
                                checkedStartTime
                        );

                LocalTime end =
                        LocalTime.parse(
                                checkedEndTime
                        );

                if (!end.isAfter(start)) {

                    return fail(
                            HttpStatus.BAD_REQUEST,
                            "종료 시간은 시작 시간보다 늦어야 합니다."
                    );
                }

            } catch (DateTimeParseException e) {

                return fail(
                        HttpStatus.BAD_REQUEST,
                        "시간 형식이 올바르지 않습니다."
                );
            }
        }


        try {

            CalendarEventDto eventDto =
                    new CalendarEventDto();

            eventDto.setMemberNo(
                    memberNo
            );

            eventDto.setEventDate(
                    eventDate
            );

            eventDto.setStartTime(
                    checkedStartTime
            );

            eventDto.setEndTime(
                    checkedEndTime
            );

            eventDto.setTitle(
                    title.trim()
            );

            eventDto.setContent(
                    trimToNull(content)
            );

            eventDto.setLocation(
                    trimToNull(location)
            );

            calendarService.insert(
                    eventDto
            );


            // 등록 후 해당 날짜 일정 다시 조회
            List<CalendarEventDto> eventList =
                    calendarService.findByDate(
                            memberNo,
                            eventDate
                    );

            return success(
                    "일정이 등록되었습니다.",
                    eventList
            );

        } catch (IllegalArgumentException e) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    errorMessage(
                            e,
                            "일정 입력값을 확인해주세요."
                    )
            );

        } catch (Exception e) {

            return fail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage(
                            e,
                            "일정 등록에 실패했습니다."
                    )
            );
        }
    }

    // =========================
    // 일정 수정
    // 사이트 DB와 구글 캘린더를 함께 수정
    // =========================
    @PostMapping("/events/update")
    public ResponseEntity<Map<String, Object>> updateEvent(
            @RequestParam int no,
            @RequestParam String eventDate,
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            HttpSession session
    ) {

        // 로그인 회원 번호 조회
        Integer memberNo =
                getMemberNo(session);


        // 로그인, 날짜 형식, 구글 연동 여부 검사
        ResponseEntity<Map<String, Object>> checkResult =
                checkRequest(
                        memberNo,
                        eventDate
                );

        if (checkResult != null) {
            return checkResult;
        }


        // 수정할 일정 번호 검사
        if (no <= 0) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "수정할 일정 번호가 올바르지 않습니다."
            );
        }


        // 일정 제목 검사
        if (
                title == null
                        || title.isBlank()
        ) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "일정 제목을 입력해주세요."
            );
        }


        // 시간 입력값 앞뒤 공백 정리
        String checkedStartTime =
                trimToNull(startTime);

        String checkedEndTime =
                trimToNull(endTime);


        /*
            시작 시간과 종료 시간은
            둘 다 입력하거나 둘 다 비워야 한다.
        */
        if (
                (checkedStartTime == null)
                        != (checkedEndTime == null)
        ) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "시작 시간과 종료 시간을 모두 선택해주세요."
            );
        }


        /*
            시간이 입력된 경우
            시간 형식과 시작·종료 순서를 검사한다.
        */
        if (checkedStartTime != null) {

            try {

                LocalTime start =
                        LocalTime.parse(
                                checkedStartTime
                        );

                LocalTime end =
                        LocalTime.parse(
                                checkedEndTime
                        );

                if (!end.isAfter(start)) {

                    return fail(
                            HttpStatus.BAD_REQUEST,
                            "종료 시간은 시작 시간보다 늦어야 합니다."
                    );
                }

            } catch (DateTimeParseException e) {

                return fail(
                        HttpStatus.BAD_REQUEST,
                        "시간 형식이 올바르지 않습니다."
                );
            }
        }


        try {

            CalendarEventDto eventDto =
                    new CalendarEventDto();


            // 수정할 일정 번호
            eventDto.setNo(
                    no
            );


            // 로그인 회원 번호
            eventDto.setMemberNo(
                    memberNo
            );


            // 수정할 날짜와 시간
            eventDto.setEventDate(
                    eventDate
            );

            eventDto.setStartTime(
                    checkedStartTime
            );

            eventDto.setEndTime(
                    checkedEndTime
            );


            // 수정할 일정 내용
            eventDto.setTitle(
                    title.trim()
            );

            eventDto.setContent(
                    trimToNull(content)
            );

            eventDto.setLocation(
                    trimToNull(location)
            );


            /*
                CalendarService에서 아래 작업을 처리한다.

                - 로그인 회원 본인의 일정인지 확인
                - 구글 캘린더 일정 수정
                - 사이트 DB 일정 수정
            */
            calendarService.update(
                    eventDto
            );


            // 수정 후 해당 날짜 일정 다시 조회
            List<CalendarEventDto> eventList =
                    calendarService.findByDate(
                            memberNo,
                            eventDate
                    );


            return success(
                    "일정이 수정되었습니다.",
                    eventList
            );

        } catch (IllegalArgumentException e) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    errorMessage(
                            e,
                            "일정 수정값을 확인해주세요."
                    )
            );

        } catch (Exception e) {

            return fail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage(
                            e,
                            "일정 수정에 실패했습니다."
                    )
            );
        }
    }

    // =========================
    // 일정 삭제
    // =========================
    @PostMapping("/events/delete")
    public ResponseEntity<Map<String, Object>> deleteEvent(
            @RequestParam int no,
            @RequestParam String date,
            HttpSession session
    ) {

        Integer memberNo =
                getMemberNo(session);

        ResponseEntity<Map<String, Object>> checkResult =
                checkRequest(
                        memberNo,
                        date
                );

        if (checkResult != null) {
            return checkResult;
        }

        if (no <= 0) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "삭제할 일정 번호가 올바르지 않습니다."
            );
        }


        try {

            calendarService.delete(
                    no,
                    memberNo
            );


            // 삭제 후 해당 날짜 일정 다시 조회
            List<CalendarEventDto> eventList =
                    calendarService.findByDate(
                            memberNo,
                            date
                    );

            return success(
                    "일정이 삭제되었습니다.",
                    eventList
            );

        } catch (Exception e) {

            return fail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage(
                            e,
                            "일정 삭제에 실패했습니다."
                    )
            );
        }
    }


    // =========================
    // 구글 일정 자동 동기화
    // =========================
    @PostMapping("/auto-sync")
    public ResponseEntity<Map<String, Object>> autoSyncGoogleCalendar(
            HttpSession session
    ) {

        Integer memberNo =
                getMemberNo(session);

        if (memberNo == null) {

            return fail(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (
                !googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        )
        ) {

            return fail(
                    HttpStatus.FORBIDDEN,
                    "구글 캘린더 연결이 필요합니다."
            );
        }


        try {

            // 구글 캘린더 변경 내용을 DB에 반영
            googleCalendarService
                    .syncGoogleCalendarEvents(
                            memberNo
                    );


            // 메인 화면 일정 위젯에서 사용할 일정 조회
            List<CalendarEventDto> eventList =
                    calendarService.findTodayEvents(
                            memberNo
                    );

            return success(
                    "구글 일정이 자동 동기화되었습니다.",
                    eventList
            );

        } catch (Exception e) {

            return fail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorMessage(
                            e,
                            "구글 일정 자동 동기화에 실패했습니다."
                    )
            );
        }
    }


    // =========================
    // 메인 화면 예정 일정 조회
    // =========================
    @GetMapping("/today")
    public ResponseEntity<Map<String, Object>> findTodayEvents(
            HttpSession session
    ) {

        Integer memberNo =
                getMemberNo(session);

        if (memberNo == null) {

            return fail(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        List<CalendarEventDto> eventList =
                calendarService.findTodayEvents(
                        memberNo
                );

        return success(
                "예정 일정을 불러왔습니다.",
                eventList
        );
    }


    // =========================
    // 로그인, 날짜, 구글 연동 공통 검사
    // =========================
    private ResponseEntity<Map<String, Object>> checkRequest(
            Integer memberNo,
            String date
    ) {

        if (memberNo == null) {

            return fail(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (!isValidDate(date)) {

            return fail(
                    HttpStatus.BAD_REQUEST,
                    "날짜 형식이 올바르지 않습니다."
            );
        }

        if (
                !googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        )
        ) {

            return fail(
                    HttpStatus.FORBIDDEN,
                    "개인 일정을 사용하려면 구글 캘린더 연동이 필요합니다."
            );
        }

        return null;
    }


    // =========================
    // 로그인 회원 번호 조회
    // 기존 loginMember 세션과 Spring Security 로그인을 함께 지원
    // =========================
    private Integer getMemberNo(
            HttpSession session
    ) {

        /*
            기존 캘린더 코드가 저장한 loginMember 세션이 있으면
            그 회원 정보를 먼저 사용한다.
        */
        Object sessionMember =
                session.getAttribute(
                        "loginMember"
                );

        if (
                sessionMember
                        instanceof MemberDto loginMember
        ) {

            return Math.toIntExact(
                    loginMember.getNo()
            );
        }


        /*
            현재 로그인 기능은 Spring Security가 관리하므로
            SecurityContext에서 로그인 회원을 조회한다.
        */
        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (
                authentication == null
                        || !authentication.isAuthenticated()
                        || authentication
                        instanceof AnonymousAuthenticationToken
        ) {
            return null;
        }


        Object principal =
                authentication.getPrincipal();

        if (
                !(principal
                        instanceof CustomUserDetails loginUser)
        ) {
            return null;
        }


        MemberDto loginMember =
                loginUser.getMemberDto();

        if (
                loginMember == null
                        || loginMember.getNo() == null
        ) {
            return null;
        }


        /*
            기존 캘린더 컨트롤러와 서비스에서도 사용할 수 있도록
            Spring Security 회원 정보를 loginMember 세션에 저장한다.
        */
        session.setAttribute(
                "loginMember",
                loginMember
        );

        return Math.toIntExact(
                loginMember.getNo()
        );
    }


    // =========================
    // yyyy-MM-dd 날짜 형식 검사
    // =========================
    private boolean isValidDate(
            String date
    ) {

        if (
                date == null
                        || date.isBlank()
        ) {
            return false;
        }

        try {

            LocalDate.parse(
                    date
            );

            return true;

        } catch (DateTimeParseException e) {

            return false;
        }
    }


    // =========================
    // 빈 문자열을 null로 변경
    // =========================
    private String trimToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String trimmedValue =
                value.trim();

        return trimmedValue.isEmpty()
                ? null
                : trimmedValue;
    }


    // =========================
    // 정상 JSON 응답
    // =========================
    private ResponseEntity<Map<String, Object>> success(
            String message,
            List<CalendarEventDto> eventList
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "success",
                true
        );

        response.put(
                "message",
                message
        );

        response.put(
                "eventList",
                eventList
        );

        return ResponseEntity.ok(
                response
        );
    }


    // =========================
    // 실패 JSON 응답
    // =========================
    private ResponseEntity<Map<String, Object>> fail(
            HttpStatus status,
            String message
    ) {

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "success",
                false
        );

        response.put(
                "message",
                message
        );

        response.put(
                "eventList",
                Collections.emptyList()
        );

        return ResponseEntity
                .status(status)
                .body(response);
    }


    // =========================
    // 예외 메시지 처리
    // =========================
    private String errorMessage(
            Exception e,
            String defaultMessage
    ) {

        return e.getMessage() == null
                || e.getMessage().isBlank()
                ? defaultMessage
                : e.getMessage();
    }
}