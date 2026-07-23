package com.siyan1234.itproject2nd.calendar.controller;

import com.siyan1234.itproject2nd.calendar.service.GoogleCalendarService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;


    // =========================
    // 구글 캘린더 연동 시작
    // 역할: 구글 권한 동의 화면으로 이동
    // =========================
    @GetMapping("/google/calendar/connect")
    public String connectGoogleCalendar(
            HttpSession session
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        // 구글 callback 요청 검증용 state 생성
        String state =
                UUID.randomUUID().toString();

        session.setAttribute(
                "googleCalendarState",
                state
        );

        String googleAuthUrl =
                googleCalendarService.createGoogleAuthUrl(
                        state
                );

        return "redirect:" + googleAuthUrl;
    }


    // =========================
    // 구글 캘린더 callback
    // 역할: 구글 권한 승인 후 회원의 토큰 저장
    // =========================
    @GetMapping("/google/calendar/callback")
    public String googleCalendarCallback(
            @RequestParam(
                    value = "code",
                    required = false
            ) String code,

            @RequestParam(
                    value = "state",
                    required = false
            ) String state,

            @RequestParam(
                    value = "error",
                    required = false
            ) String error,

            HttpSession session
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        /*
            사용자가 구글 권한 동의 화면에서
            취소를 누른 경우 캘린더로 복귀
        */
        if (error != null) {
            return "redirect:/calendar";
        }

        String savedState =
                (String) session.getAttribute(
                        "googleCalendarState"
                );

        // 세션 state와 구글 callback state 비교
        if (savedState == null
                || !savedState.equals(state)) {

            throw new IllegalStateException(
                    "구글 캘린더 연동 요청이 올바르지 않습니다."
            );
        }

        session.removeAttribute(
                "googleCalendarState"
        );

        if (code == null || code.isBlank()) {
            throw new IllegalStateException(
                    "구글 인증 코드가 없습니다."
            );
        }

        Integer memberNo =
                loginMember.getNo();

        googleCalendarService.saveGoogleToken(
                code,
                memberNo
        );

        return "redirect:/calendar";
    }


    // =========================
    // 구글 캘린더 일정 동기화
    // 역할: 연동 회원만 구글 개인 일정을 동기화
    // =========================
    @GetMapping("/google/calendar/sync")
    public String syncGoogleCalendar(
            @RequestParam(
                    required = false
            ) String date,

            HttpSession session,
            Model model
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        Integer memberNo =
                loginMember.getNo();

        boolean googleConnected =
                googleCalendarService
                        .isGoogleCalendarConnected(
                                memberNo
                        );

        /*
            구글 미연동 상태에서는 동기화를 실행하지 않고
            전용 안내 화면으로 이동
        */
        if (!googleConnected) {

            model.addAttribute(
                    "date",
                    date
            );

            return "calendar/google-required";
        }

        googleCalendarService
                .syncGoogleCalendarEvents(
                        memberNo
                );

        /*
            동기화 버튼에 날짜가 전달된 경우
            기존에 보고 있던 날짜로 복귀
        */
        if (date != null && !date.isBlank()) {
            return "redirect:/calendar?date=" + date;
        }

        return "redirect:/calendar";
    }


    // =========================
    // 구글 캘린더 연동 해제
    // 역할: 로그인 회원의 구글 토큰 정보 삭제
    // =========================
    @GetMapping("/google/calendar/disconnect")
    public String disconnectGoogleCalendar(
            HttpSession session
    ) {

        MemberDto loginMember =
                (MemberDto) session.getAttribute("loginMember");

        if (loginMember == null) {
            return "redirect:/member/login";
        }

        Integer memberNo =
                loginMember.getNo();

        googleCalendarService
                .disconnectGoogleCalendar(
                        memberNo
                );

        return "redirect:/calendar";
    }
}