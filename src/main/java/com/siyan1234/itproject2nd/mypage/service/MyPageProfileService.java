package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.mypage.dao.MyPageDao;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageProfileDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageUpdateDto;
import com.siyan1234.itproject2nd.mypage.support.MyPageMessages;
import com.siyan1234.itproject2nd.mypage.support.MyPagePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** 마이페이지의 기본 프로필과 보안 개인정보 변경을 담당합니다. */
@Service
@RequiredArgsConstructor
public class MyPageProfileService {

    private final MyPageDao myPageDao;
    private final MyPageQueryService myPageQueryService;

    /** 내 정보 탭의 이름·닉네임 수정입니다. */
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

    /** 보안 설정 탭의 이메일·전화번호·생년월일·주소 수정입니다. */
    @Transactional
    public MyPageActionResponseDto updateSecurityProfile(
            Integer memberNo,
            MyPageUpdateDto updateDto,
            boolean securityVerified,
            String verificationFailureMessage
    ) {
        MyPageProfileDto currentProfile = myPageQueryService.findProfile(memberNo);
        if (currentProfile == null) {
            return failByMemberNo(memberNo);
        }

        if (updateDto == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.INVALID_REQUEST);
        }

        if (requiresPasswordVerification(currentProfile, securityVerified)) {
            String message = verificationFailureMessage == null
                    ? MyPageMessages.SECURITY_PASSWORD_VERIFY_REQUIRED
                    : verificationFailureMessage;
            return MyPageActionResponseDto.fail(message);
        }

        normalizeSecurityProfile(updateDto);

        String validationMessage = validateSecurityProfile(memberNo, updateDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.updateSecurityProfile(memberNo, updateDto);
        return successWithMyPage(MyPageMessages.SECURITY_PROFILE_UPDATED, memberNo);
    }

    private MyPageActionResponseDto checkMemberReady(Integer memberNo) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.LOGIN_INFO_MISSING);
        }

        if (myPageQueryService.findProfile(memberNo) == null) {
            return MyPageActionResponseDto.fail(MyPageMessages.MEMBER_NOT_FOUND);
        }

        return null;
    }

    private MyPageActionResponseDto failByMemberNo(Integer memberNo) {
        return MyPageActionResponseDto.fail(
                memberNo == null ? MyPageMessages.LOGIN_INFO_MISSING : MyPageMessages.MEMBER_NOT_FOUND
        );
    }

    private MyPageActionResponseDto successWithMyPage(String message, Integer memberNo) {
        return MyPageActionResponseDto.success(message, myPageQueryService.getMyPage(memberNo));
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
