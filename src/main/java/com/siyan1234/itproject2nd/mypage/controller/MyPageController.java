package com.siyan1234.itproject2nd.mypage.controller;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageVerifyPasswordDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import com.siyan1234.itproject2nd.mypage.service.MyPageService;
import com.siyan1234.itproject2nd.mypage.support.MyPageLoginUserResolver;
import com.siyan1234.itproject2nd.mypage.support.MyPageMessages;
import com.siyan1234.itproject2nd.mypage.support.MyPageSecurityVerification;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/mypage")
public class MyPageController {

    private final MyPageService myPageService;
    private final MyPageLoginUserResolver loginUserResolver;

    /**
     * 메인 홈 계정 위젯과 마이페이지 모달에서 공통으로 사용하는 현재 로그인 회원 정보입니다.
     * 비로그인 상태에서는 예외가 아니라 loggedIn=false 응답을 반환합니다.
     */
    @GetMapping("/me")
    public MyPageResponseDto myPage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return MyPageResponseDto.anonymous();
        }

        return myPageService.getMyPage(memberNo);
    }

    /**
     * 내 정보 탭의 공개성 낮은 기본 정보 수정입니다.
     * 이름/닉네임처럼 별도 비밀번호 재확인이 필요하지 않은 항목만 처리합니다.
     */
    @PostMapping("/profile")
    public ResponseEntity<MyPageActionResponseDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageUpdateDto updateDto
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);
        if (memberNo == null) {
            return unauthorized();
        }

        return ok(myPageService.updateProfile(memberNo, updateDto));
    }

    /**
     * 보안 설정 탭에서 개인정보를 표시/수정하기 전 일반 로그인 회원의 현재 비밀번호를 확인합니다.
     * 소셜 로그인 회원은 사이트 비밀번호가 없으므로 Service에서 안내 메시지를 반환합니다.
     */
    @PostMapping("/verify-password")
    public ResponseEntity<MyPageActionResponseDto> verifyPassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageVerifyPasswordDto verifyDto,
            HttpSession session
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);
        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.verifyPassword(memberNo, verifyDto);
        if (responseDto.isSuccess()) {
            MyPageSecurityVerification.markVerified(session, memberNo);
        }

        return ok(responseDto);
    }

    /**
     * 보안 설정 탭의 개인정보 수정입니다.
     * 일반 회원은 verify-password 성공 세션이 있어야 하고, 소셜 회원은 현재 로그인 세션 기준으로 수정합니다.
     */
    @PostMapping("/security")
    public ResponseEntity<MyPageActionResponseDto> updateSecurityProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageUpdateDto updateDto,
            HttpSession session
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);
        if (memberNo == null) {
            return unauthorized();
        }

        String verificationFailureMessage = MyPageSecurityVerification.verificationFailureMessage(session, memberNo);
        boolean securityVerified = verificationFailureMessage == null;
        return ok(myPageService.updateSecurityProfile(memberNo, updateDto, securityVerified, verificationFailureMessage));
    }

    /**
     * 비밀번호 변경 탭 전용 요청입니다.
     * 소셜 로그인 회원은 Service에서 차단하고 안내 메시지를 반환합니다.
     */
    @PostMapping("/password")
    public ResponseEntity<MyPageActionResponseDto> changePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody PasswordChangeDto passwordDto,
            HttpSession session
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);
        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.changePassword(memberNo, passwordDto);
        if (responseDto.isSuccess()) {
            MyPageSecurityVerification.clear(session);
        }

        return ok(responseDto);
    }

    /**
     * 회원 탈퇴 요청입니다.
     * 탈퇴 성공 시 SecurityContext와 세션을 모두 정리해서 즉시 로그아웃 상태로 전환합니다.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<MyPageActionResponseDto> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageWithdrawDto withdrawDto,
            HttpSession session
    ) {
        Integer memberNo = loginUserResolver.resolveMemberNo(customUserDetails, authentication);
        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.withdraw(memberNo, withdrawDto);
        if (responseDto.isSuccess()) {
            SecurityContextHolder.clearContext();
            session.invalidate();
        }

        return ok(responseDto);
    }

    private ResponseEntity<MyPageActionResponseDto> ok(MyPageActionResponseDto responseDto) {
        return ResponseEntity.ok(responseDto);
    }

    private ResponseEntity<MyPageActionResponseDto> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MyPageActionResponseDto.fail(MyPageMessages.LOGIN_REQUIRED));
    }
}
