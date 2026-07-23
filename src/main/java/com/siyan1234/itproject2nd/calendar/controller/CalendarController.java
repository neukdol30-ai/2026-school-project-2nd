package com.siyan1234.itproject2nd.calendar.controller;

import com.siyan1234.itproject2nd.calendar.dto.CalendarDayDto;
import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import com.siyan1234.itproject2nd.calendar.service.CalendarService;
import com.siyan1234.itproject2nd.calendar.service.GoogleCalendarService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    // 주소: /calendar
    // =========================
    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(required = false) String date,

            @RequestParam(
                    required = false,
                    defaultValue = "false"
            ) boolean googleRequired,

            @AuthenticationPrincipal
            CustomUserDetails loginUser,

            Model model
    ) {

        MemberDto loginMember =
                getLoginMember(
                        loginUser
                );

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(
                        loginMember.getNo()
                );

        LocalDate selectedDate =
                date == null || date.isBlank()
                        ? LocalDate.now()
                        : LocalDate.parse(date);

        String selectedDateText =
                selectedDate.toString();

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        List<CalendarDayDto> calendarList =
                calendarService
                        .makeMonthCalendarWithEvents(
                                selectedDate,
                                memberNo,
                                googleConnected
                        );

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

        model.addAttribute(
                "googleConnected",
                googleConnected
        );

        model.addAttribute(
                "googleRequired",
                googleRequired
        );

        return "calendar/calendar";
    }


    // =========================
    // 미니 캘린더 날짜 클릭
    // 주소: /calendar/day?date=yyyy-MM-dd
    // =========================
    @GetMapping("/calendar/day")
    public String calendarDay(
            @RequestParam String date,

            @AuthenticationPrincipal
            CustomUserDetails loginUser
    ) {

        MemberDto loginMember =
                getLoginMember(
                        loginUser
                );

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        return "redirect:/calendar?date=" + date;
    }


    // =========================
    // 일정 등록 화면
    // =========================
    @GetMapping("/calendar/write")
    public String calendarWrite(
            @RequestParam String date,

            @AuthenticationPrincipal
            CustomUserDetails loginUser,

            Model model
    ) {

        MemberDto loginMember =
                getLoginMember(
                        loginUser
                );

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(
                        loginMember.getNo()
                );

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        if (!googleConnected) {
            return "redirect:/calendar?date="
                    + date
                    + "&googleRequired=true";
        }

        model.addAttribute(
                "date",
                date
        );

        return "calendar/write";
    }


    // =========================
    // 일정 등록 처리
    // =========================
    @PostMapping("/calendar/write")
    public String calendarWritePost(
            @RequestParam String eventDate,

            @RequestParam String title,

            @RequestParam(required = false)
            String content,

            @RequestParam(required = false)
            String location,

            @AuthenticationPrincipal
            CustomUserDetails loginUser
    ) {

        MemberDto loginMember =
                getLoginMember(
                        loginUser
                );

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(
                        loginMember.getNo()
                );

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

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

        calendarService.insert(
                calendarEventDto
        );

        return "redirect:/calendar?date="
                + eventDate;
    }


    // =========================
    // 일정 삭제 처리
    // =========================
    @PostMapping("/calendar/delete")
    public String calendarDelete(
            @RequestParam int no,

            @RequestParam String date,

            @AuthenticationPrincipal
            CustomUserDetails loginUser
    ) {

        MemberDto loginMember =
                getLoginMember(
                        loginUser
                );

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        int memberNo =
                Math.toIntExact(
                        loginMember.getNo()
                );

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        if (!googleConnected) {
            return "redirect:/calendar?date="
                    + date
                    + "&googleRequired=true";
        }

        calendarService.delete(
                no,
                memberNo
        );

        return "redirect:/calendar?date="
                + date;
    }


    // =========================
    // 로그인 회원 조회
    // 팀원 로그인 방식인 Spring Security 인증 정보를 사용
    // =========================
    private MemberDto getLoginMember(
            CustomUserDetails loginUser
    ) {

        if (loginUser == null) {
            return null;
        }

        return loginUser.getMemberDto();
    }
}