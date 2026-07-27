package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.mypage.dao.MyPageDao;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageVerifyPasswordDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import com.siyan1234.itproject2nd.mypage.support.MyPageMessages;
import com.siyan1234.itproject2nd.mypage.support.MyPagePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 마이페이지의 현재 비밀번호 확인과 비밀번호 변경을 담당합니다. */
@Service
@RequiredArgsConstructor
public class MyPagePasswordService {

    private final MemberDao memberDao;
    private final MyPageDao myPageDao;
    private final MyPageQueryService myPageQueryService;
    private final PasswordEncoder passwordEncoder;

    /** 보안 설정 탭의 개인정보 표시·수정 전 현재 비밀번호를 확인합니다. */
    @Transactional(readOnly = true)
    public MyPageActionResponseDto verifyPassword(Integer memberNo, MyPageVerifyPasswordDto verifyDto) {
        MemberDto member = findMember(memberNo);
        if (member == null) {
            return failByMemberNo(memberNo);
        }

        if (myPageQueryService.isSocialLoginUser(memberNo)) {
            return MyPageActionResponseDto.fail(MyPageMessages.SOCIAL_PASSWORD_VERIFY_NOT_REQUIRED);
        }

        String passwordMessage = validateCurrentPassword(
                member,
                verifyDto == null ? null : verifyDto.getCurrentPassword()
        );
        if (passwordMessage != null) {
            return MyPageActionResponseDto.fail(passwordMessage);
        }

        return successWithMyPage(MyPageMessages.PASSWORD_VERIFIED, memberNo);
    }

    /** 비밀번호 변경 탭의 비밀번호 변경 처리입니다. */
    @Transactional
    public MyPageActionResponseDto changePassword(Integer memberNo, PasswordChangeDto passwordDto) {
        MemberDto member = findMember(memberNo);
        if (member == null) {
            return failByMemberNo(memberNo);
        }

        if (myPageQueryService.isSocialLoginUser(memberNo)) {
            return MyPageActionResponseDto.fail(MyPageMessages.SOCIAL_PASSWORD_CHANGE_NOT_ALLOWED);
        }

        String validationMessage = validatePasswordChange(member, passwordDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        String encodedPassword = passwordEncoder.encode(passwordDto.getNewPassword());
        myPageDao.updatePassword(memberNo, encodedPassword);
        return successWithMyPage(MyPageMessages.PASSWORD_CHANGED, memberNo);
    }

    private MemberDto findMember(Integer memberNo) {
        if (memberNo == null) {
            return null;
        }
        return memberDao.findByNo(memberNo);
    }

    private MyPageActionResponseDto failByMemberNo(Integer memberNo) {
        return MyPageActionResponseDto.fail(
                memberNo == null ? MyPageMessages.LOGIN_INFO_MISSING : MyPageMessages.MEMBER_NOT_FOUND
        );
    }

    private MyPageActionResponseDto successWithMyPage(String message, Integer memberNo) {
        return MyPageActionResponseDto.success(message, myPageQueryService.getMyPage(memberNo));
    }

    private String validatePasswordChange(MemberDto member, PasswordChangeDto passwordDto) {
        String currentPasswordMessage = validateCurrentPassword(
                member,
                passwordDto == null ? null : passwordDto.getCurrentPassword()
        );
        if (currentPasswordMessage != null) {
            return currentPasswordMessage;
        }

        if (!MyPagePolicy.isValidPassword(passwordDto.getNewPassword())) {
            return MyPageMessages.NEW_PASSWORD_INVALID;
        }

        if (!passwordDto.getNewPassword().equals(passwordDto.getNewPasswordCheck())) {
            return MyPageMessages.NEW_PASSWORD_CHECK_MISMATCH;
        }

        if (passwordEncoder.matches(passwordDto.getNewPassword(), member.getPassword())) {
            return MyPageMessages.SAME_PASSWORD_NOT_ALLOWED;
        }

        return null;
    }

    private String validateCurrentPassword(MemberDto member, String currentPassword) {
        if (MyPagePolicy.isBlank(currentPassword)) {
            return MyPageMessages.CURRENT_PASSWORD_REQUIRED;
        }

        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            return MyPageMessages.CURRENT_PASSWORD_MISMATCH;
        }

        return null;
    }
}
