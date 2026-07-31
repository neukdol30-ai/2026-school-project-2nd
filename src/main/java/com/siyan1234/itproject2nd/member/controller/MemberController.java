package com.siyan1234.itproject2nd.member.controller;

import com.siyan1234.itproject2nd.config.handler.CustomLoginFailureHandler;
import com.siyan1234.itproject2nd.config.security.PasswordPolicy;
import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.dto.*;
import com.siyan1234.itproject2nd.member.service.KakaoUnlinkService;
import com.siyan1234.itproject2nd.member.service.MailService;
import com.siyan1234.itproject2nd.member.service.MemberService;
import com.siyan1234.itproject2nd.member.service.PendingSocialSignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Security;


@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService; // 회원가입 로직 처리 Service

    private final MailService mailService; // build.gradle mail 스타터 + RedisConfig가 만든 StringRedisTemplate 내부적으로 사용.

    // Redis 소셜 가입 대기정보 조회, 삭제 담당 (동의 완료 후 정리용)
    private final PendingSocialSignupService pendingSocialSignupService;

    // 카카오 서버 쪽 연결(연동) 자체를 끊는 API 호출 담당
    private final KakaoUnlinkService kakaoUnlinkService;

    // 아이디, 비밀번호 찾기 재작업
    private static final String FIND_ID_RESULT_SESSION_KEY = "findIdResultMemberId";

    private static final String RESET_PW_EMAIL_SESSION_KEY = "resetPwVerifiedEmail";

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
                         BindingResult bindingResult,
                         Model model) { // 회원가입 폼 제출 처리

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

    // POST /member/signup/send-code : 회원가입 이메일 인증번호 발송
    @PostMapping("/signup/send-code")
    @ResponseBody
    public String sendSignupAuthCode(@RequestParam("email") String email) {

        try {
            mailService.sendAuthCode(email, MailService.MailPurpose.SIGNUP);
        } catch (MailService.MailCooldownException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            log.error("회원가입 인증번호 발송 실패 email={}", email, e);
            return "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        }

        return "인증번호를 발송했습니다.";
    }

    // POST /member/signup/signup/verify-code : 회원가입 이메일 인증번호 확인
    @PostMapping("/signup/verify-code")
    @ResponseBody
    public boolean verifySignupAuthCode(@RequestParam("email") String email,
                                        @RequestParam("code") String code) {
        return mailService.verifyAuthCode(email, MailService.MailPurpose.SIGNUP, code);
    }

    // GET /member/signup/email-verified : 새로고침 시 "이 이메일 이미 인증됐나?"를 서버에 재확인
    // 브라우저 저장소(sessionStorage)만이 아닌 Redis의 실제 인증 기록을 다시 조회
    @GetMapping("/signup/email-verified")
    @ResponseBody
    public boolean checkSignupEmailVerified(@RequestParam("email") String email) {
        return mailService.isVerified(email, MailService.MailPurpose.SIGNUP);
    }

    @GetMapping("/login")
    public String loginForm(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            HttpSession session, // 영준
            Model model // 영준
    ) { // 로그인 화면 보여줌

        if (loginUser != null) { // 이미 로그인한 사용자인지 확인
            return "redirect:/"; // 로그인 상태라면 로그인 화면 대신 메인으로 보냄.
        }

        Object loginErrorMessage = session.getAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
        if (loginErrorMessage != null) {
            model.addAttribute("loginErrorMessage", loginErrorMessage);
            session.removeAttribute(CustomLoginFailureHandler.LOGIN_ERROR_MESSAGE_SESSION_KEY);
        } // 영준

        return "member/login";
    }

    // GET /member/terms 요청으로 이용약관 전문 화면 보여줌
    @GetMapping("/terms")
    public String termsPage() {

        return "member/terms";
    }

    // GET /member/privacy 요청으로 개인정보 수집·이용 전문 화면 보여줌
    @GetMapping("/privacy")
    public String privacyPage() {

        return "member/privacy";
    }

    // GET /member/terms-agree 요청 -> 신규 소셜 회원 약관 동의 화면 보여줌
    @GetMapping("/terms-agree")
    public String termsAgreeForm(
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        // 1단계 : 로그인 자체가 안 된 경우만 우선 차단. 대기/정식 구분은 아직 하지 않음
        if (loginUser == null || loginUser.getMemberDto() == null) {

            return "redirect:" + SecurityPaths.MEMBER_LOGIN;
        }

        // 2단계 : 가입 대기 상태(소셜 인증만 끝나고 DB엔 없는 상태) -> no 없는 게 정상, no 검사보다 먼저 통과시켜 약관 화면 보여줌
        if (loginUser.isPendingSocialSignup()) {

            return "member/terms-agree";
        }

        // 3단계 : 여기부터는 대기 상태가 아닌 "정식으로 DB에 있는 회원". -> no 없으면 비정상
        if (loginUser.getMemberDto().getNo() == null) {

            return "redirect:" + SecurityPaths.MEMBER_LOGIN;
        }

        MemberDto memberDto = loginUser.getMemberDto();

        // 4단계 : 두 약관에 이미 동의한 회원은 약관 화면 다시 볼 필요 X
        if (isAgreementCompleted(memberDto)) {

            return "redirect:" + SecurityPaths.HOME;
        }

        return "member/terms-agree";
    }

    // POST /member/terms-agree 요청으로 두 약관의 실제 동의 처리 수행
    @PostMapping("/terms-agree")
    public String termsAgreeProcess(@AuthenticationPrincipal CustomUserDetails loginUser,
                                    @RequestParam(value = "agreeTermsYn", required = false) String agreeTermsYn,
                                    @RequestParam(value = "agreePrivacyYn", required = false) String agreePrivacyYn,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes
    ) {

        // 1단계 : 로그인 자체 안 된 경우만 우선 차단
        if (loginUser == null || loginUser.getMemberDto() == null) {

            return "redirect:" + SecurityPaths.MEMBER_LOGIN;
        }

        // 2단계 : HTML의 required 속성을 우회한 직접 POST 요청까지 서버에서 다시 검증
        if (!"Y".equals(agreeTermsYn)
                || !"Y".equals(agreePrivacyYn)) {

            redirectAttributes.addFlashAttribute(
                    "agreementError",
                    "이용약관과 개인정보 수집·이용에 모두 동의해야 합니다."
            );

            return "redirect:" + SecurityPaths.MEMBER_TERMS_AGREE;
        }

        // 3단계 : 가입 대기 상태면 이번이 진짜 회원가입을 확정하는 시점
        if (loginUser.isPendingSocialSignup()) {

            String pendingToken = loginUser.getPendingSocialToken();

            MemberDto savedMember = memberService.completeSocialSignup(pendingToken);

            if (savedMember == null) {

                redirectAttributes.addFlashAttribute(
                        "agreementError",
                        "회원가입 처리에 실패했습니다. 소셜 로그인을 다시 시도해 주세요."
                );

                return "redirect:" + SecurityPaths.MEMBER_LOGIN;
            }

            // 저장 끝났으니 Redis 임시정보는 필요 없음 -> 즉시 삭제(TTL 10분 기다리지 않음)
            pendingSocialSignupService.delete(pendingToken);

            redirectAttributes.addFlashAttribute(
                    "toastMessage",
                    "회원가입이 완료되었습니다. 다시 로그인해 주세요."
            );

            SecurityContextHolder.clearContext(); // 이번 요청의 인증 정보 제거
            session.invalidate(); // 세션 자체를 폐기

            return "redirect:" + SecurityPaths.MEMBER_LOGIN;
        }

        // 4단계 : 여기부터는 대기 상태가 아닌 정식 회원. no가 없으면 비정상
        if (loginUser.getMemberDto().getNo() == null) {

            return "redirect:" + SecurityPaths.MEMBER_LOGIN;
        }

        MemberDto memberDto = loginUser.getMemberDto();

        boolean updated = memberService.updateAgreement(memberDto.getNo());

        if (!updated) {

            redirectAttributes.addFlashAttribute(
                    "agreementError",
                    "약관 동의 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요."
            );

            return "redirect:" + SecurityPaths.MEMBER_TERMS_AGREE;
        }

        memberDto.setAgreeTermsYn("Y");

        memberDto.setAgreePrivacyYn("Y");

        return "redirect:" + SecurityPaths.HOME;
    }

    // POST /member/terms-agree/cancel : 신규 소셜 회원이 약관에 동의하지 않고 가입을 취소할 때 처리
    // SecurityPaths.MEMBER_TERMS_AGREE_CANCEL 상수가 가리키는 실제 처리 지점
    @PostMapping("/terms-agree/cancel")
    public String termsAgreeCancel(@AuthenticationPrincipal CustomUserDetails loginUser,
                                   HttpSession session) {

        // 가입 대기 상태일 때만 Redis에 지울 개인정보가 있음
        if (loginUser != null && loginUser.isPendingSocialSignup()) {

            String pendingToken = loginUser.getPendingSocialToken();

            // Redis를 지우기 전에 먼저 조회해서 provider, providerId 확보
            PendingSocialSignupDto pendingInfo = pendingSocialSignupService.find(pendingToken);

            // provider가 카카오면 카카오 서버 쪽 연결 자체도 끊음
            if (pendingInfo != null && "kakao".equals(pendingInfo.getProvider())) {

                boolean unlinked = kakaoUnlinkService.unlinkByAdminKey(pendingInfo.getProviderId());

                // 카카오 서버 장애 등으로 실패해도 로그아웃 자체는 막지 않는다 (실패해도 계속 진행)
                if (!unlinked) {

                    log.warn("카카오 연결 해제 실패. 가입 취소는 계속 진행함. providerId={}",
                            pendingInfo.getProviderId());
                }
            }
            // 네이버는 로컬 DB만 정리 대상이라 여기서 API 호출 없음.
            pendingSocialSignupService.delete(pendingToken);
        }

        SecurityContextHolder.clearContext(); // 이번 요청의 인증 정보 제거

        session.invalidate(); // 세션 자체를 폐기

        return "redirect:" + SecurityPaths.MEMBER_LOGIN;
    }

    // true/false(boolean) 그대로 브라우저 전달. JS가 이 값을 받아 메시지 띄움.
    @GetMapping("/exists")
    @ResponseBody
    public boolean checkMemberIdDuplicate(@RequestParam("memberId") String memberId) {
        return memberService.isMemberIdDuplicate(memberId); // true=중복, false=사용 가능
    }

    // true/false(boolean) 그대로 브라우저 전달. JS가 값을 받아 메시지 띄움
    @GetMapping("/exists-nickname")
    @ResponseBody
    public boolean checkNicknameDuplicate(@RequestParam("nickname") String nickname) {
        return memberService.isNicknameDuplicate(nickname); // true=중복, false=사용 가능
    }

    // 아이디 찾기
    // GET /member/find-id : 아이디 찾기 화면(이메일 입력 화면)을 보여줌
    @GetMapping("/find-id")
    public String findIdForm(@AuthenticationPrincipal CustomUserDetails loginUser) { // @AuthenticationPrincipal : 현재 로그인한 사용자의 인증 정보.

        if (loginUser != null) { // 이미 로그인한 사용자
            return "redirect:/"; // 이미 아이디를 아는 상태. 찾기 화면 대신 메인으로.
        } // 거짓이면 건너뛰고 아래 진행(비로그인 사용자 -> 찾기 화면 정상 진입)

        return "member/find-id"; // templates/member/find-id.html
    }

    // POST /member/find-id/send : 사용자가 입력한 이메일로 인증번호 메일 발송
    @PostMapping("/find-id/send")
    @ResponseBody // 화면 이름 대신 return 값을 그대로 HTTP 응답 본문(텍스트)으로 브라우저에 돌려보냄.
    public String sendFindIdAuthCode(@RequestParam("email") String email) {
        // find-id.html의 이메일 입력칸.

        MemberDto foundMember = memberService.findByEmail(email); // 이메일이 실제 가입된 회원인지 먼저 확인

        if (foundMember == null) { // 가입 내역 없는 이메일.
            return "가입된 회원 정보와 일치하지 않는 이메일입니다."; // 메일 발송 자체를 하지 않고 바로 안내
        }

        try {
            mailService.sendAuthCode(email, MailService.MailPurpose.FIND_ID);
        } catch (MailService.MailCooldownException e) {
            // 더 구체적인 예외를 먼저 catch해야 함. - 순서 바꾸면 컴파일 오류
            return e.getMessage();
        } catch (RuntimeException e) {
            // SMTP 접속 실패 등 그 외 모든 예상치 못한 발송 실패
            log.error("아이디 찾기 인증번호 발송 실패 email={}", email, e); // 콘솔에 원인 스택트레이스까지 기록(개발자용)
            return "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."; // 화면에는 원인 대신 안내 문구만(사용자용)
        }

        return "인증번호를 발송했습니다.";
    }

    // POST /member/find-id/Verify : 사용자가 입력한 인증번호가 맞는지 확인
    @PostMapping("/find-id/verify")
    @ResponseBody
    public boolean verifyFindIdAuthCode(@RequestParam("email") String email,
                                        @RequestParam("code") String code,
                                        HttpSession session) {

        boolean verified = mailService.verifyAuthCode(email, MailService.MailPurpose.FIND_ID, code);

        if (!verified) { // 인증번호 틀렸거나 만료
            return false; // 검증 실패를 그대로 브라우저에 알림.
        }

        MemberDto foundMember = memberService.findByEmail(email); // 인증 성공한 시점에 다시 조회, 시간차로 탈퇴 등 상태 변경될 수 있으니.

        if (foundMember == null) { // 이론상 거의 없음. 방어적으로 재확인
            return false;
        }

        session.setAttribute(FIND_ID_RESULT_SESSION_KEY, foundMember.getMemberId()); // 이 코드가 세션에 직접 써넣음.(Spring 자동 X)

        return true;
    }

    // GET /member/find-id/result : 인증 성공 후, 찾은 아이디를 1회 보여주는 화면
    @GetMapping("/find-id/result")
    public String findIdResult(HttpSession session, Model model) {

        Object resultMemberId = session.getAttribute(FIND_ID_RESULT_SESSION_KEY);
        // 반환 타입 Object : 세션엔 어떤 타입이든 저장 가능

        if (resultMemberId == null) {
            return "redirect:/member/find-id";
        }

        String memberId = (String) resultMemberId; // Object -> String 형변환

        boolean isSocialMember = memberId.startsWith("kakao_") || memberId.startsWith("naver_");

        String socialProviderName = null; // 일반 회원이면 null 유지

        if (memberId.startsWith("kakao_")) {
            socialProviderName = "카카오";
        } else if (memberId.startsWith("naver_")) {
            socialProviderName = "네이버";
        }

        model.addAttribute("resultMemberId", resultMemberId); // find-id-result.html에서 ${resultMemberId}로 사용
        model.addAttribute("isSocialMember", isSocialMember); // 신규 : 화면 분기용
        model.addAttribute("socialProviderName", socialProviderName); // 신규 : "카카오"/"네이버"/null

        session.removeAttribute(FIND_ID_RESULT_SESSION_KEY);
        // 1회성 처리 : 결과 화면을 새로고침하거나 뒤로가기로 재방문해도 값이 다시 안 보이게 즉시 삭제

        return "member/find-id-result";
    }

    // 비밀번호 찾기 / 재설정

    // GET /member/find-password : 비밀번호 찾기 화면(아이디 + 이메일 입력 화면)을 보여줌
    @GetMapping("/find-password")
    public String findPasswordForm(@AuthenticationPrincipal CustomUserDetails loginUser) {
        if (loginUser != null) {
            return "redirect:/";
        }
        return "member/find-password";
    }

    // POST /member/find-password/send : 아이디 + 이메일 본인 확인 후 인증번호 발송
    @PostMapping("/find-password/send")
    @ResponseBody
    public String sendFindPasswordAuthCode(@RequestParam("memberId") String memberId,
                                           @RequestParam("email") String email) {

        MemberDto foundMember = memberService.findByMemberIdAndEmail(memberId, email);
        // 여기선 이메일 하나만 아니라 "아이디 + 이메일이 같은 사람것인지"까지 확인

        if (foundMember == null) { // 아이디와 이메일 조합이 DB에 없음.
            return "아이디와 이메일이 일치하는 회원이 없습니다."; // 발송 안 함.
        }


        // find-id/send와 동일한 이유
        try {
            mailService.sendAuthCode(email, MailService.MailPurpose.RESET_PW);
        } catch (MailService.MailCooldownException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            log.error("비밀번호 찾기 인증번호 발송 실패 email={}", email, e);
            return "메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.";
        }

        return "인증번호를 발송했습니다.";
    }

    // POST /member/find-password/verify : 인증번호 확인 후, 비밀번호 재설정 접근 권한을 세션에 기록
    @PostMapping("/find-password/verify")
    @ResponseBody
    public boolean verifyFindPasswordAuthCode(@RequestParam("email") String email,
                                              @RequestParam("code") String code,
                                              HttpSession session) {

        boolean verified = mailService.verifyAuthCode(email, MailService.MailPurpose.RESET_PW, code);

        if (verified) { // 인증번호 정확히 일치
            session.setAttribute(RESET_PW_EMAIL_SESSION_KEY, email);
            // 참이면 실행 -> 이 브라우저는 이 이메일에 대해 방금 인증 통과라는 사실을 세션에 남김.
        }

        return verified;
    }

    // GET/member/reset-password : 새 비밀번호 입력 화면 (인증을 거치지 않고는 못 들어온다)
    @GetMapping("/reset-password")
    public String resetPasswordForm(HttpSession session) {

        Object verifiedEmail = session.getAttribute(RESET_PW_EMAIL_SESSION_KEY);

        if (verifiedEmail == null) { // 인증 절차 없이 주소를 직접 입력해 들어왔다
            return "redirect:/member/find-password"; // 참이면 인증 화면으로
        }

        return "member/reset-password";
    }

    // POST /member/reset-password : 실제 비밀번호 변경 처리
    @PostMapping("/reset-password")
    public String resetPasswordProcess(@Valid @ModelAttribute("resetPasswordDto") ResetPasswordDto resetPasswordDto,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {

        Object verifiedEmailObj = session.getAttribute(RESET_PW_EMAIL_SESSION_KEY);

        if (verifiedEmailObj == null) { // 방어적 재확인 : 세션 만료 등으로 중간에 인증 상태가 풀렸을 수 있음
            return "redirect:/member/find-password";
        }

        String verifiedEmail = (String) verifiedEmailObj; // 형변환

        if (bindingResult.hasErrors()) {
            model.addAttribute("resetPasswordError", bindingResult.getFieldError().getDefaultMessage());
            // getFieldError() : 여러 오류 중 첫 번째 하나 / getDefaultMessage() : DTO의 @Pattern(message=...) 문구
            return "member/reset-password";
        }

        if (!resetPasswordDto.getNewPassword().equals(resetPasswordDto.getNewPasswordCheck())) {
            model.addAttribute("resetPasswordError", "비밀번호가 일치하지 않습니다.");
            return "member/reset-password";
        }

        boolean updated = memberService.updatePasswordByEmail(verifiedEmail, resetPasswordDto.getNewPassword());
        // updatePasswordByEmail 내부의 isPasswordValid() 재검사는 그대로 둔다.

        if (!updated) {
            model.addAttribute("resetPasswordError", PasswordPolicy.PASSWORD_MESSAGE);
            return "member/reset-password";
        }

        mailService.clearVerified(verifiedEmail, MailService.MailPurpose.RESET_PW);
        // 인증 완료 상태(Redis)를 지워서, 같은 인증으로 비밀번호를 두 번 바꾸지 못하게 막음
        session.removeAttribute(RESET_PW_EMAIL_SESSION_KEY); // 세션 쪽 인증 표시도 함께 제거

        redirectAttributes.addFlashAttribute("toastMessage", "비밀번호가 변경되었습니다. 다시 로그인해 주세요.");

        return "redirect:/member/login";
    }

    // 현재 회원이 이용약관과 개인정보 수집·이용에 모두 동의했는지 확인
    private boolean isAgreementCompleted(MemberDto memberDto) {

        boolean termsAgreed = "Y".equalsIgnoreCase(memberDto.getAgreeTermsYn());

        boolean privacyAgreed = "Y".equalsIgnoreCase(memberDto.getAgreePrivacyYn());

        return termsAgreed && privacyAgreed;
    }
}