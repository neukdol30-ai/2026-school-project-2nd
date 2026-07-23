package com.siyan1234.itproject2nd.calendar.controller;

import com.siyan1234.itproject2nd.calendar.dto.CalendarDayDto;
import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import com.siyan1234.itproject2nd.calendar.service.CalendarService;
import com.siyan1234.itproject2nd.calendar.service.GoogleCalendarService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final GoogleCalendarService googleCalendarService;


    // =========================
    // 월간 캘린더 화면
    // 역할: 공휴일은 항상 표시하고, 구글 연동 회원만 개인 일정을 표시
    // 주소: /calendar, /calendar?date=2026-07-21
    // =========================
    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(required = false) String date,
            @RequestParam(
                    required = false,
                    defaultValue = "false"
            ) boolean googleRequired,
            HttpSession session,
            Model model
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        // 사이트 로그인하지 않은 사용자는 로그인 화면으로 이동
        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(loginMember.getNo());

        // 주소에 날짜가 없으면 오늘 날짜 사용
        LocalDate selectedDate =
                (date == null || date.isBlank())
                        ? LocalDate.now()
                        : LocalDate.parse(date);

        String selectedDateText =
                selectedDate.toString();

        /*
            구글 캘린더 연동 여부 확인

            true:
            공휴일 + 개인 일정 사용 가능

            false:
            공휴일만 표시
        */
        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(memberNo);

        /*
            월간 달력 생성

            CalendarService에서 대한민국 공휴일 정보를
            월간 날짜 목록에 포함한다.
        */
        List<CalendarDayDto> calendarList =
                calendarService.makeMonthCalendarWithEvents(
                        selectedDate,
                        memberNo,
                        googleConnected
                );

        /*
            구글 연동 회원만 개인 일정 목록 조회

            연동하지 않은 회원은 빈 목록을 전달하여
            개인 일정이 화면에 표시되지 않게 한다.
        */
        List<CalendarEventDto> eventList =
                googleConnected
                        ? calendarService.findByDate(
                        memberNo,
                        selectedDateText
                )
                        : Collections.emptyList();

        model.addAttribute(
                "year",
                selectedDate.getYear()
        );

        model.addAttribute(
                "month",
                selectedDate.getMonthValue()
        );

        model.addAttribute(
                "selectedDate",
                selectedDateText
        );

        model.addAttribute(
                "calendarList",
                calendarList
        );

        model.addAttribute(
                "eventList",
                eventList
        );

        // HTML에서 구글 연동 버튼과 일정 기능 표시 여부를 결정
        model.addAttribute(
                "googleConnected",
                googleConnected
        );

        /*
            연동하지 않은 상태에서 일정 작성이나 삭제를 시도했는지 전달
            나중에 HTML에서 안내 문구를 표시할 때 사용
        */
        model.addAttribute(
                "googleRequired",
                googleRequired
        );

        return "calendar/calendar";
    }


    // =========================
    // 기존 날짜 상세 주소 연결
    // 역할: 예전 /calendar/day 주소를 현재 /calendar 주소로 이동
    // =========================
    @GetMapping("/calendar/day")
    public String calendarDay(
            @RequestParam String date,
            HttpSession session
    ) {

        if (session.getAttribute("loginMember") == null) {
            return "redirect:/member/login";
        }

        return "redirect:/calendar?date=" + date;
    }


    // =========================
    // 일정 등록 화면
    // 역할: 구글 연동 회원만 일정 작성 화면에 접근
    // 주소: /calendar/write?date=2026-07-14
    // =========================
    @GetMapping("/calendar/write")
    public String calendarWrite(
            @RequestParam String date,
            HttpSession session,
            Model model
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(loginMember.getNo());

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(memberNo);

        /*
            구글 캘린더를 연동하지 않은 회원은
            일정 작성 화면에 들어가지 못하게 한다.
        */
        if (!googleConnected) {
            return "redirect:/calendar?date="
                    + date
                    + "&googleRequired=true";
        }

        model.addAttribute("date", date);

        return "calendar/write";
    }


    // =========================
    // 일정 등록 처리
    // 역할: 구글 연동 회원의 일정을 사이트와 구글에 함께 등록
    // =========================
    @PostMapping("/calendar/write")
    public String calendarWritePost(
            @RequestParam String eventDate,
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @RequestParam(required = false) String location,
            HttpSession session
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(loginMember.getNo());

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(memberNo);

        /*
            화면을 거치지 않고 주소나 요청을 직접 보내더라도
            구글 미연동 회원은 일정을 등록할 수 없게 막는다.
        */
        if (!googleConnected) {
            return "redirect:/calendar?date="
                    + eventDate
                    + "&googleRequired=true";
        }

        CalendarEventDto calendarEventDto =
                new CalendarEventDto();

        calendarEventDto.setMemberNo(memberNo);
        calendarEventDto.setEventDate(eventDate);
        calendarEventDto.setTitle(title);
        calendarEventDto.setContent(content);
        calendarEventDto.setLocation(location);

        /*
            CalendarService에서 아래 작업을 함께 처리한다.

            - 구글 캘린더 일정 생성
            - 구글 일정 ID 저장
            - 우리 사이트 DB 일정 저장
        */
        calendarService.insert(calendarEventDto);

        return "redirect:/calendar?date=" + eventDate;
    }


    // =========================
    // 일정 삭제 처리
    // 역할: 구글 연동 회원의 사이트 일정과 구글 일정을 함께 삭제
    // =========================
    @PostMapping("/calendar/delete")
    public String calendarDelete(
            @RequestParam int no,
            @RequestParam String date,
            HttpSession session
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(loginMember.getNo());

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(memberNo);

        /*
            구글 캘린더 미연동 상태에서는
            개인 일정 삭제 요청도 실행하지 않는다.
        */
        if (!googleConnected) {
            return "redirect:/calendar?date="
                    + date
                    + "&googleRequired=true";
        }

        /*
            CalendarService에서 아래 작업을 처리한다.

            - 연결된 구글 일정 삭제
            - 우리 사이트 일정 IS_DELETED = 'Y' 처리
            - 로그인 회원 본인의 일정만 삭제
        */
        calendarService.delete(no, memberNo);

        return "redirect:/calendar?date=" + date;
    }
}