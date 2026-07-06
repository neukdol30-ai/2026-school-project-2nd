package com.siyan1234.itproject2nd.member.member_controller;

import com.siyan1234.itproject2nd.member.member_dto.PortalMemberDto;
import com.siyan1234.itproject2nd.member.member_service.PortalMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class PortalMemberController {

    private final PortalMemberService portalMemberService;

    // 회원가입 페이지 이동
    @GetMapping("/signup")
    public String signup() {

        // templates/member/signup.html
        return "member/signup";
    }

    // 회원가입 처리
    @PostMapping("/signup")
    public String signupProcess(PortalMemberDto portalMemberDto,
                                RedirectAttributes redirectAttributes) {

        try {
            // 회원가입 실행
            portalMemberService.signup(portalMemberDto);

            // 성공 메시지
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "회원가입이 완료되었습니다."
            );

            // 로그인 담당자가 만든 로그인 페이지로 이동
            return "redirect:/member/login";

        } catch (IllegalArgumentException e) {

            // 실패 메시지
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            // 실패하면 다시 회원가입 페이지
            return "redirect:/member/signup";
        }
    }

    // 아이디 중복확인
    // GET으로 만든 이유: 중복확인은 조회라서 POST보다 덜 꼬임
    @ResponseBody
    @GetMapping("/id-check")
    public Map<String, Boolean> idCheck(@RequestParam(required = false) String memberId) {

        boolean isDuplicate =
                portalMemberService.isDuplicateMemberId(memberId);

        return Collections.singletonMap("isDuplicate", isDuplicate);
    }
}