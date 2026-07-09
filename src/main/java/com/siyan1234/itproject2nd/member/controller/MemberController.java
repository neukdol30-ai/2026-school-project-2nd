package com.siyan1234.itproject2nd.member.controller;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import com.siyan1234.itproject2nd.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
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
    public String signup(@Valid @ModelAttribute("signupDto") SignupDto signupDto,
                         BindingResult bindingResult) { // 회원가입 폼 제출 처리

        log.info("회원가입 요청 아이디 = {}", signupDto.getMemberId()); // 폼에서 아이디 넘어왔는지 확인
        log.info("회원가입 요청 이메일 = {}", signupDto.getEmail()); // 폼에서 이메일 넘어왔는지 확인
        log.info("약관 동의 값 = {}", signupDto.getAgreeTermsYn()); // 약관 체크박스 값 Y로 넘어왔는지 확인
        log.info("개인정보 동의 값 = {}", signupDto.getAgreePrivacyYn()); // 개인정보 체크박스 값 Y로 넘어왔는지 확인

        boolean hasErrors = memberService.hasSignupErrors(signupDto, bindingResult); // Service에서 회원가입 검증 실행

        if (hasErrors) {
            bindingResult.getFieldErrors().forEach(error -> { // 필드별 오류 목록 하나식 꺼내기
                log.warn("회원가입 필드 오류 field={}, rejectedValue={}, message={}",
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()); // 어떤 필드 문제인지 콘솔 출력
            });

            return "member/signup"; // 오류 있으면 다시 회원가입으로
        }

        memberService.signup(signupDto); // 오류 없었으면 회원가입 정보 DB에 저장

        return "redirect:/member/login?signup=success"; // 가입 성공 후 로그인 화면으로 이동
    }

    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal CustomUserDetails loginUser) { // 로그인 화면 보여줌

        if (loginUser != null) { // 이미 로그인한 사용자인지 확인
            return "redirect:/"; // 로그인 상태라면 로그인 화면 대신 메인으로 보냄.
        }

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
