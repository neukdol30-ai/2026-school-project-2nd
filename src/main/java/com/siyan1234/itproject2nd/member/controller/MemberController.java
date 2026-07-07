package com.siyan1234.itproject2nd.member.controller;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService; // 회원가입 로직 처리 Service

    @GetMapping("/signup")
    public String signupForm(Model model, @AuthenticationPrincipal CustomUserDetails loginUser) { // 회원가입 화면 보여줌

        if (loginUser != null) { // 이미 로그인한 사용자인지 확인
            return "redirect:/"; // 로그인 상태 -> 메인 화면으로
        }

        model.addAttribute("signupDto", new SignupDto()); // 빈 회원가입 DTO를 화면에 전달

        return "member/signup"; // templates/member/signup.html 보여줌
    }

    @PostMapping("/signup") // POST /member/signup 요청 처리
    public String signup(@Valid SignupDto signupDto, BindingResult bindingResult) { // 회원가입 폼 제출 처리

        if (memberService.hasSignupErrors(signupDto,bindingResult)) { // 검증 오류 있는지 Service에서 확인
            return "member/signup"; // 오류 있다면 -> 다시 회원가입 화면으로
        }

        memberService.signup(signupDto); // 오류 없다면 -> 회원가입을 DB에 저장

        return "redirect:/member/login?signup=success"; // 가입 성공 후 로그인 화면으로 이동
    }

    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal CustomUserDetails loginUser) { // 로그인 화면 보여줌

        if (loginUser != null) { // 이미 로그인한 사용자인지 확인
            return "redirect:/"; // 로그인 상태라면 로그인 화면 대신 메인으로 보냄.
        }

        return "member/login";
    }

}
