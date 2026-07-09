package com.siyan1234.itproject2nd.member.controller;

import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TempLoginController {

    @GetMapping("/temp/user-login")
    public String tempUserLogin(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = request.getSession(true);

        MemberDto memberDto = new MemberDto();
        memberDto.setNo(1);
        memberDto.setMemberId("user01");
        memberDto.setName("일반사용자");
        memberDto.setNickname("사용자");
        memberDto.setRole("USER");

        session.setAttribute("loginUser", memberDto);

        return "redirect:/chat";
    }

    @GetMapping("/temp/admin-login")
    public String tempAdminLogin(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }

        HttpSession session = request.getSession(true);

        MemberDto memberDto = new MemberDto();
        memberDto.setNo(2);
        memberDto.setMemberId("admin01");
        memberDto.setName("관리자");
        memberDto.setNickname("관리자");
        memberDto.setRole("ADMIN");

        session.setAttribute("loginUser", memberDto);

        return "redirect:/chat/admin";
    }

    @GetMapping("/temp/logout")
    public String tempLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}