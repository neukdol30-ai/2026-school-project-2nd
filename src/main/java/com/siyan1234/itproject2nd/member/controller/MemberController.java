package com.siyan1234.itproject2nd.member.controller;

import com.siyan1234.itproject2nd.member.dto.LoginDto;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/login")
    public String login() {
        return "member/login";
    }

    @PostMapping("/login")
    public String loginProcess(LoginDto loginDto,
                               HttpSession session,
                               Model model) {

        MemberDto loginMember = memberService.login(loginDto);

        if (loginMember == null) {
            model.addAttribute("error", "아이디 또는 비밀번호가 틀렸습니다.");
            return "member/login";
        }

        session.setAttribute("loginMember", loginMember);

        return "redirect:/";
    }

    @GetMapping("/signup")
    public String signup() {
        return "member/signup";
    }

    @PostMapping("/signup")
    public String signupProcess(MemberDto memberDto,
                                RedirectAttributes redirectAttributes) {
        try {
            memberService.signup(memberDto);
            return "redirect:/member/login";

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/member/signup";
        }
    }

    @ResponseBody
    @GetMapping("/id-check")
    public Map<String, Boolean> idCheck(@RequestParam(required = false) String memberId) {

        boolean isDuplicate = memberService.isDuplicateMemberId(memberId);

        return Collections.singletonMap("isDuplicate", isDuplicate);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
