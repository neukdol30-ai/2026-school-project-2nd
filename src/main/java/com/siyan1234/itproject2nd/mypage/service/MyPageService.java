package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageVerifyPasswordDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 마이페이지 Controller가 사용하는 진입점 Service입니다.
 *
 * <p>기존 Controller API를 유지하면서 조회, 프로필, 비밀번호, 탈퇴 책임을
 * 각각의 전용 Service로 위임합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageQueryService myPageQueryService;
    private final MyPageProfileService myPageProfileService;
    private final MyPagePasswordService myPagePasswordService;
    private final MyPageWithdrawalService myPageWithdrawalService;

    public MyPageResponseDto getMyPage(Integer memberNo) {
        return myPageQueryService.getMyPage(memberNo);
    }

    public MyPageActionResponseDto updateProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        return myPageProfileService.updateProfile(memberNo, updateDto);
    }

    public MyPageActionResponseDto verifyPassword(Integer memberNo, MyPageVerifyPasswordDto verifyDto) {
        return myPagePasswordService.verifyPassword(memberNo, verifyDto);
    }

    public MyPageActionResponseDto updateSecurityProfile(
            Integer memberNo,
            MyPageUpdateDto updateDto,
            boolean securityVerified,
            String verificationFailureMessage
    ) {
        return myPageProfileService.updateSecurityProfile(
                memberNo,
                updateDto,
                securityVerified,
                verificationFailureMessage
        );
    }

    public MyPageActionResponseDto changePassword(Integer memberNo, PasswordChangeDto passwordDto) {
        return myPagePasswordService.changePassword(memberNo, passwordDto);
    }

    public MyPageActionResponseDto withdraw(Integer memberNo, MyPageWithdrawDto withdrawDto) {
        return myPageWithdrawalService.withdraw(memberNo, withdrawDto);
    }
}
