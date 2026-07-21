package com.siyan1234.itproject2nd.mypage.controller;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageVerifyPasswordDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import com.siyan1234.itproject2nd.mypage.service.MyPageService;
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

    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final String SECURITY_VERIFIED_MEMBER_NO = "MYPAGE_SECURITY_VERIFIED_MEMBER_NO";

    private final MyPageService myPageService;
    private final MemberDao memberDao;

    @GetMapping("/me")
    public MyPageResponseDto myPage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return MyPageResponseDto.anonymous();
        }

        return myPageService.getMyPage(memberNo);
    }

    @PostMapping("/profile")
    public ResponseEntity<MyPageActionResponseDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageUpdateDto updateDto
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.updateProfile(memberNo, updateDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/verify-password")
    public ResponseEntity<MyPageActionResponseDto> verifyPassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageVerifyPasswordDto verifyDto,
            HttpSession session
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.verifyPassword(memberNo, verifyDto);
        if (responseDto.isSuccess()) {
            session.setAttribute(SECURITY_VERIFIED_MEMBER_NO, memberNo);
        }
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/security")
    public ResponseEntity<MyPageActionResponseDto> updateSecurityProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageUpdateDto updateDto,
            HttpSession session
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.updateSecurityProfile(
                memberNo,
                updateDto,
                isSecurityVerified(session, memberNo)
        );
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/password")
    public ResponseEntity<MyPageActionResponseDto> changePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody PasswordChangeDto passwordDto
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.changePassword(memberNo, passwordDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<MyPageActionResponseDto> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Authentication authentication,
            @RequestBody MyPageWithdrawDto withdrawDto,
            HttpSession session
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails, authentication);

        if (memberNo == null) {
            return unauthorized();
        }

        MyPageActionResponseDto responseDto = myPageService.withdraw(memberNo, withdrawDto);

        if (responseDto.isSuccess()) {
            SecurityContextHolder.clearContext();
            session.invalidate();
        }

        return ResponseEntity.ok(responseDto);
    }

    /**
     * 일반 폼 로그인과 소셜 로그인 모두 마이페이지에서 동일하게 처리하기 위한 로그인 회원번호 조회입니다.
     *
     * @AuthenticationPrincipal 이 null로 들어오는 경우에도 Authentication의 name(memberId)을 기준으로
     * DB에서 한 번 더 조회해서 메인 화면 로그인 상태 표시가 누락되지 않도록 보강했습니다.
     */
    private Integer resolveMemberNo(CustomUserDetails customUserDetails, Authentication authentication) {
        Integer memberNo = resolveFromCustomUserDetails(customUserDetails);
        if (memberNo != null) {
            return memberNo;
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String principalText && ANONYMOUS_USER.equals(principalText)) {
            return null;
        }

        if (principal instanceof CustomUserDetails customPrincipal) {
            return resolveFromCustomUserDetails(customPrincipal);
        }

        String memberId = authentication.getName();
        if (memberId == null || memberId.isBlank() || ANONYMOUS_USER.equals(memberId)) {
            return null;
        }

        MemberDto memberDto = memberDao.findByMemberId(memberId);
        return memberDto == null ? null : memberDto.getNo();
    }

    private Integer resolveFromCustomUserDetails(CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return null;
        }

        MemberDto memberDto = customUserDetails.getMemberDto();
        return memberDto == null ? null : memberDto.getNo();
    }

    private boolean isSecurityVerified(HttpSession session, Integer memberNo) {
        Object verifiedMemberNo = session.getAttribute(SECURITY_VERIFIED_MEMBER_NO);
        return memberNo != null && memberNo.equals(verifiedMemberNo);
    }

    private ResponseEntity<MyPageActionResponseDto> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MyPageActionResponseDto.fail("로그인이 필요합니다."));
    }
}
