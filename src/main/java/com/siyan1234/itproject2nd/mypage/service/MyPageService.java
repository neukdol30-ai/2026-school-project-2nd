package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.mypage.dao.MyPageDao;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageProfileDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageVerifyPasswordDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.dto.PasswordChangeDto;
import com.siyan1234.itproject2nd.mypage.support.MyPageMessages;
import com.siyan1234.itproject2nd.mypage.support.MyPagePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final MyPageDao myPageDao;
    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    /** 마이페이지 모달을 처음 열 때 필요한 모든 사용자 정보를 한 번에 조회합니다. */
    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(Integer memberNo) {
        MyPageProfileDto profile = findProfile(memberNo);
        if (profile == null) {
            return MyPageResponseDto.anonymous();
        }

        return MyPageResponseDto.loggedIn(
                profile,
                myPageDao.findActivityByMemberNo(memberNo),
                myPageDao.findRecentBoards(memberNo),
                myPageDao.findRecentChats(memberNo)
        );
    }

    /** 내 정보 탭의 기본 정보 수정입니다. */
    @Transactional
    public MyPageActionResponseDto updateProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        MyPageActionResponseDto readyCheck = checkMemberReady(memberNo);
        if (readyCheck != null) {
            return readyCheck;
        }

        if (updateDto == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.INVALID_REQUEST);
        }

        normalizeBasicProfile(updateDto);

        String validationMessage = validateBasicProfile(memberNo, updateDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.updateBasicProfile(memberNo, updateDto);
        return successWithMyPage(MyPageMessages.PROFILE_UPDATED, memberNo);
    }

    /** 보안 설정 탭의 개인정보 표시/수정 전 현재 비밀번호를 확인합니다. */
    @Transactional(readOnly = true)
    public MyPageActionResponseDto verifyPassword(Integer memberNo, MyPageVerifyPasswordDto verifyDto) {
        MemberDto member = findMember(memberNo);
        if (member == null) {
            return failByMemberNo(memberNo);
        }

        if (isSocialLoginUser(memberNo)) {
            return MyPageActionResponseDto.fail(MyPageMessages.SOCIAL_PASSWORD_VERIFY_NOT_REQUIRED);
        }

        String passwordMessage = validateCurrentPassword(member, verifyDto == null ? null : verifyDto.getCurrentPassword());
        if (passwordMessage != null) {
            return MyPageActionResponseDto.fail(passwordMessage);
        }

        return successWithMyPage(MyPageMessages.PASSWORD_VERIFIED, memberNo);
    }

    /** 보안 설정 탭의 개인정보 수정입니다. */
    @Transactional
    public MyPageActionResponseDto updateSecurityProfile(Integer memberNo, MyPageUpdateDto updateDto, boolean securityVerified) {
        MyPageProfileDto currentProfile = findProfile(memberNo);
        if (currentProfile == null) {
            return failByMemberNo(memberNo);
        }

        if (updateDto == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.INVALID_REQUEST);
        }

        if (requiresPasswordVerification(currentProfile, securityVerified)) {
            return MyPageActionResponseDto.fail(MyPageMessages.SECURITY_PASSWORD_VERIFY_REQUIRED);
        }

        normalizeSecurityProfile(updateDto);

        String validationMessage = validateSecurityProfile(memberNo, updateDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.updateSecurityProfile(memberNo, updateDto);
        return successWithMyPage(MyPageMessages.SECURITY_PROFILE_UPDATED, memberNo);
    }

    /** 비밀번호 변경 탭의 비밀번호 변경 처리입니다. */
    @Transactional
    public MyPageActionResponseDto changePassword(Integer memberNo, PasswordChangeDto passwordDto) {
        MemberDto member = findMember(memberNo);
        if (member == null) {
            return failByMemberNo(memberNo);
        }

        if (isSocialLoginUser(memberNo)) {
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

    /** 회원 탈퇴 탭의 회원 탈퇴 처리입니다. */
    @Transactional
    public MyPageActionResponseDto withdraw(Integer memberNo, MyPageWithdrawDto withdrawDto) {
        MemberDto member = findMember(memberNo);
        if (member == null) {
            return failByMemberNo(memberNo);
        }

        String validationMessage = validateWithdraw(memberNo, member, withdrawDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.deleteMe(memberNo);
        return MyPageActionResponseDto.successWithRedirect(MyPageMessages.WITHDRAW_COMPLETED, "/");
    }

    private MyPageActionResponseDto checkMemberReady(Integer memberNo) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.LOGIN_INFO_MISSING);
        }

        if (findProfile(memberNo) == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.MEMBER_NOT_FOUND);
        }

        return null;
    }

    private MyPageActionResponseDto failByMemberNo(Integer memberNo) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.LOGIN_INFO_MISSING);
        }

        return MyPageActionResponseDto.fail(MyPageMessages.MEMBER_NOT_FOUND);
    }

    private MyPageActionResponseDto successWithMyPage(String message, Integer memberNo) {
        return MyPageActionResponseDto.success(message, getMyPage(memberNo));
    }

    private MemberDto findMember(Integer memberNo) {
        if (memberNo == null) {
            return null;
        }

        return memberDao.findByNo(memberNo);
    }

    private MyPageProfileDto findProfile(Integer memberNo) {
        if (memberNo == null) {
            return null;
        }

        return myPageDao.findProfileByNo(memberNo);
    }

    private boolean isSocialLoginUser(Integer memberNo) {
        MyPageProfileDto profile = findProfile(memberNo);
        return profile != null && profile.isSocialLoginUser();
    }

    private boolean requiresPasswordVerification(MyPageProfileDto profile, boolean securityVerified) {
        return !profile.isSocialLoginUser() && !securityVerified;
    }

    private String validateBasicProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        if (MyPagePolicy.isBlank(updateDto.getName())) {
            return MyPageMessages.NAME_REQUIRED;
        }

        if (MyPagePolicy.isBlank(updateDto.getNickname())) {
            return MyPageMessages.NICKNAME_REQUIRED;
        }

        if (updateDto.getNickname().length() < 2 || updateDto.getNickname().length() > 10) {
            return MyPageMessages.NICKNAME_LENGTH_INVALID;
        }

        if (myPageDao.countNicknameDuplicateExceptMe(memberNo, updateDto.getNickname()) > 0) {
            return MyPageMessages.NICKNAME_DUPLICATED;
        }

        return null;
    }

    private String validateSecurityProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        if (!MyPagePolicy.isBlank(updateDto.getEmail())) {
            if (!MyPagePolicy.isValidEmail(updateDto.getEmail())) {
                return MyPageMessages.EMAIL_INVALID;
            }

            if (myPageDao.countEmailDuplicateExceptMe(memberNo, updateDto.getEmail()) > 0) {
                return MyPageMessages.EMAIL_DUPLICATED;
            }
        }

        if (!MyPagePolicy.isBlank(updateDto.getPhone()) && !MyPagePolicy.isValidPhone(updateDto.getPhone())) {
            return MyPageMessages.PHONE_INVALID;
        }

        if (updateDto.getBirthDate() != null && updateDto.getBirthDate().isAfter(LocalDate.now())) {
            return MyPageMessages.BIRTH_DATE_INVALID;
        }

        if (!MyPagePolicy.isValidGender(updateDto.getGender())) {
            return MyPageMessages.GENDER_INVALID;
        }

        return null;
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

    private String validateWithdraw(Integer memberNo, MemberDto member, MyPageWithdrawDto withdrawDto) {
        if (MyPagePolicy.isAdminRole(member.getRole())) {
            return MyPageMessages.ADMIN_WITHDRAW_NOT_ALLOWED;
        }

        if (withdrawDto == null || !MyPagePolicy.isWithdrawConfirmText(withdrawDto.getConfirmText())) {
            return MyPageMessages.WITHDRAW_CONFIRM_REQUIRED;
        }

        if (isSocialLoginUser(memberNo)) {
            return null;
        }

        if (MyPagePolicy.isBlank(withdrawDto.getPassword())) {
            return MyPageMessages.WITHDRAW_PASSWORD_REQUIRED;
        }

        if (!passwordEncoder.matches(withdrawDto.getPassword(), member.getPassword())) {
            return MyPageMessages.CURRENT_PASSWORD_MISMATCH;
        }

        return null;
    }

    private void normalizeBasicProfile(MyPageUpdateDto updateDto) {
        updateDto.setName(MyPagePolicy.trimToNull(updateDto.getName()));
        updateDto.setNickname(MyPagePolicy.trimToNull(updateDto.getNickname()));
    }

    private void normalizeSecurityProfile(MyPageUpdateDto updateDto) {
        updateDto.setEmail(MyPagePolicy.trimToNull(updateDto.getEmail()));
        updateDto.setPhone(MyPagePolicy.trimToNull(updateDto.getPhone()));
        updateDto.setGender(MyPagePolicy.trimToNull(updateDto.getGender()));
        updateDto.setPostcode(MyPagePolicy.trimToNull(updateDto.getPostcode()));
        updateDto.setAddress(MyPagePolicy.trimToNull(updateDto.getAddress()));
        updateDto.setDetailAddress(MyPagePolicy.trimToNull(updateDto.getDetailAddress()));
    }
}
