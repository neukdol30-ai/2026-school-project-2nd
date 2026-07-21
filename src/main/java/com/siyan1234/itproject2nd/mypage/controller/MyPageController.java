package com.siyan1234.itproject2nd.mypage.controller;

import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import com.siyan1234.itproject2nd.mypage.service.MyPageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/me")
    public MyPageResponseDto myPage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails);

        if (memberNo == null) {
            return MyPageResponseDto.anonymous();
        }

        return myPageService.getMyPage(memberNo);
    }

    @PostMapping("/profile")
    public ResponseEntity<MyPageActionResponseDto> updateProfile(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody MyPageUpdateDto updateDto
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails);

        if (memberNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MyPageActionResponseDto.fail("로그인이 필요합니다."));
        }

        MyPageActionResponseDto responseDto = myPageService.updateProfile(memberNo, updateDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/password")
    public ResponseEntity<MyPageActionResponseDto> changePassword(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody PasswordChangeDto passwordDto
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails);

        if (memberNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MyPageActionResponseDto.fail("로그인이 필요합니다."));
        }

        MyPageActionResponseDto responseDto = myPageService.changePassword(memberNo, passwordDto);
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<MyPageActionResponseDto> withdraw(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody MyPageWithdrawDto withdrawDto,
            HttpSession session
    ) {
        Integer memberNo = resolveMemberNo(customUserDetails);

        if (memberNo == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MyPageActionResponseDto.fail("로그인이 필요합니다."));
        }

        MyPageActionResponseDto responseDto = myPageService.withdraw(memberNo, withdrawDto);

        if (responseDto.isSuccess()) {
            SecurityContextHolder.clearContext();
            session.invalidate();
        }

        return ResponseEntity.ok(responseDto);
    }

    private Integer resolveMemberNo(CustomUserDetails customUserDetails) {
        if (customUserDetails == null) {
            return null;
        }

        MemberDto memberDto = customUserDetails.getMemberDto();
        return memberDto == null ? null : memberDto.getNo();
    }
}
