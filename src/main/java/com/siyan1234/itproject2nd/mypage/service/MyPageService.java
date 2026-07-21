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
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String PASSWORD_PATTERN = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,20}";
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private static final String PHONE_PATTERN = "^010-[0-9]{4}-[0-9]{4}$";
    private static final String WITHDRAW_CONFIRM_TEXT = "회원탈퇴";

    private final MyPageDao myPageDao;
    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public MyPageResponseDto getMyPage(Integer memberNo) {
        MyPageProfileDto profile = myPageDao.findProfileByNo(memberNo);

        MyPageResponseDto responseDto = new MyPageResponseDto();
        responseDto.setLoggedIn(profile != null);

        if (profile == null) {
            return responseDto;
        }

        responseDto.setProfile(profile);
        responseDto.setActivity(myPageDao.findActivityByMemberNo(memberNo));
        responseDto.setRecentBoards(myPageDao.findRecentBoards(memberNo));
        responseDto.setRecentChats(myPageDao.findRecentChats(memberNo));

        return responseDto;
    }

    @Transactional
    public MyPageActionResponseDto updateProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }

        MyPageProfileDto currentProfile = myPageDao.findProfileByNo(memberNo);
        if (currentProfile == null) {
            return MyPageActionResponseDto.fail("회원 정보를 찾을 수 없습니다.");
        }

        normalizeBasicProfile(updateDto);

        String validationMessage = validateBasicProfile(memberNo, updateDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.updateBasicProfile(memberNo, updateDto);
        return MyPageActionResponseDto.success("내 정보가 수정되었습니다.", getMyPage(memberNo));
    }

    @Transactional(readOnly = true)
    public MyPageActionResponseDto verifyPassword(Integer memberNo, MyPageVerifyPasswordDto verifyDto) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }

        if (isSocialLoginUser(memberNo)) {
            return MyPageActionResponseDto.fail("소셜 로그인 계정은 사이트 비밀번호 인증이 필요하지 않습니다.");
        }

        MemberDto member = memberDao.findByNo(memberNo);
        if (member == null) {
            return MyPageActionResponseDto.fail("회원 정보를 찾을 수 없습니다.");
        }

        if (verifyDto == null || isBlank(verifyDto.getCurrentPassword())) {
            return MyPageActionResponseDto.fail("현재 비밀번호를 입력해 주세요.");
        }

        if (!passwordEncoder.matches(verifyDto.getCurrentPassword(), member.getPassword())) {
            return MyPageActionResponseDto.fail("현재 비밀번호가 일치하지 않습니다.");
        }

        return MyPageActionResponseDto.success("본인 확인이 완료되었습니다.", getMyPage(memberNo));
    }

    @Transactional
    public MyPageActionResponseDto updateSecurityProfile(Integer memberNo, MyPageUpdateDto updateDto, boolean securityVerified) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }

        MyPageProfileDto currentProfile = myPageDao.findProfileByNo(memberNo);
        if (currentProfile == null) {
            return MyPageActionResponseDto.fail("회원 정보를 찾을 수 없습니다.");
        }

        if (!currentProfile.isSocialLoginUser() && !securityVerified) {
            return MyPageActionResponseDto.fail("개인정보 수정을 위해 현재 비밀번호 인증이 필요합니다.");
        }

        normalizeSecurityProfile(updateDto);

        String validationMessage = validateSecurityProfile(memberNo, updateDto);
        if (validationMessage != null) {
            return MyPageActionResponseDto.fail(validationMessage);
        }

        myPageDao.updateSecurityProfile(memberNo, updateDto);
        return MyPageActionResponseDto.success("개인정보가 수정되었습니다.", getMyPage(memberNo));
    }

    @Transactional
    public MyPageActionResponseDto changePassword(Integer memberNo, PasswordChangeDto passwordDto) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }

        MemberDto member = memberDao.findByNo(memberNo);
        if (member == null) {
            return MyPageActionResponseDto.fail("회원 정보를 찾을 수 없습니다.");
        }

        if (isSocialLoginUser(memberNo)) {
            return MyPageActionResponseDto.fail("소셜 로그인 계정은 사이트에서 비밀번호를 변경할 수 없습니다. 카카오/네이버 계정 설정에서 관리해 주세요.");
        }

        if (passwordDto == null || isBlank(passwordDto.getCurrentPassword())) {
            return MyPageActionResponseDto.fail("현재 비밀번호를 입력해 주세요.");
        }

        if (!passwordEncoder.matches(passwordDto.getCurrentPassword(), member.getPassword())) {
            return MyPageActionResponseDto.fail("현재 비밀번호가 일치하지 않습니다.");
        }

        if (!isValidPassword(passwordDto.getNewPassword())) {
            return MyPageActionResponseDto.fail("새 비밀번호는 대문자, 소문자, 숫자를 포함한 8~20자로 입력해 주세요.");
        }

        if (!passwordDto.getNewPassword().equals(passwordDto.getNewPasswordCheck())) {
            return MyPageActionResponseDto.fail("새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (passwordEncoder.matches(passwordDto.getNewPassword(), member.getPassword())) {
            return MyPageActionResponseDto.fail("현재 사용 중인 비밀번호와 같은 비밀번호로는 변경할 수 없습니다.");
        }

        String encodedPassword = passwordEncoder.encode(passwordDto.getNewPassword());
        myPageDao.updatePassword(memberNo, encodedPassword);

        return MyPageActionResponseDto.success("비밀번호가 변경되었습니다.", getMyPage(memberNo));
    }

    @Transactional
    public MyPageActionResponseDto withdraw(Integer memberNo, MyPageWithdrawDto withdrawDto) {
        if (memberNo == null) {
            return MyPageActionResponseDto.fail("로그인 정보가 없습니다. 다시 로그인해 주세요.");
        }

        MemberDto member = memberDao.findByNo(memberNo);
        if (member == null) {
            return MyPageActionResponseDto.fail("회원 정보를 찾을 수 없습니다.");
        }

        if (ADMIN_ROLE.equalsIgnoreCase(member.getRole())) {
            return MyPageActionResponseDto.fail("관리자 계정은 마이페이지에서 탈퇴할 수 없습니다. 관리자 콘솔에서 관리해 주세요.");
        }

        if (withdrawDto == null || !WITHDRAW_CONFIRM_TEXT.equals(withdrawDto.getConfirmText())) {
            return MyPageActionResponseDto.fail("회원 탈퇴를 진행하려면 확인 문구에 '회원탈퇴'를 정확히 입력해 주세요.");
        }

        if (!isSocialLoginUser(memberNo)) {
            if (isBlank(withdrawDto.getPassword())) {
                return MyPageActionResponseDto.fail("회원 탈퇴를 위해 현재 비밀번호를 입력해 주세요.");
            }

            if (!passwordEncoder.matches(withdrawDto.getPassword(), member.getPassword())) {
                return MyPageActionResponseDto.fail("현재 비밀번호가 일치하지 않습니다.");
            }
        }

        myPageDao.deleteMe(memberNo);
        return MyPageActionResponseDto.successWithRedirect("회원 탈퇴가 완료되었습니다.", "/");
    }

    private boolean isSocialLoginUser(Integer memberNo) {
        MyPageProfileDto profile = myPageDao.findProfileByNo(memberNo);
        return profile != null && profile.isSocialLoginUser();
    }

    private void normalizeBasicProfile(MyPageUpdateDto updateDto) {
        updateDto.setName(trimToNull(updateDto.getName()));
        updateDto.setNickname(trimToNull(updateDto.getNickname()));
    }

    private void normalizeSecurityProfile(MyPageUpdateDto updateDto) {
        updateDto.setEmail(trimToNull(updateDto.getEmail()));
        updateDto.setPhone(trimToNull(updateDto.getPhone()));
        updateDto.setGender(trimToNull(updateDto.getGender()));
        updateDto.setPostcode(trimToNull(updateDto.getPostcode()));
        updateDto.setAddress(trimToNull(updateDto.getAddress()));
        updateDto.setDetailAddress(trimToNull(updateDto.getDetailAddress()));
    }

    private String validateBasicProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        if (isBlank(updateDto.getName())) {
            return "이름을 입력해 주세요.";
        }

        if (isBlank(updateDto.getNickname())) {
            return "닉네임을 입력해 주세요.";
        }

        if (updateDto.getNickname().length() < 2 || updateDto.getNickname().length() > 10) {
            return "닉네임은 2~10자로 입력해 주세요.";
        }

        if (myPageDao.countNicknameDuplicateExceptMe(memberNo, updateDto.getNickname()) > 0) {
            return "이미 사용 중인 닉네임입니다.";
        }

        return null;
    }

    private String validateSecurityProfile(Integer memberNo, MyPageUpdateDto updateDto) {
        if (!isBlank(updateDto.getEmail())) {
            if (!updateDto.getEmail().matches(EMAIL_PATTERN)) {
                return "이메일 형식이 올바르지 않습니다.";
            }

            if (myPageDao.countEmailDuplicateExceptMe(memberNo, updateDto.getEmail()) > 0) {
                return "이미 사용 중인 이메일입니다.";
            }
        }

        if (!isBlank(updateDto.getPhone()) && !updateDto.getPhone().matches(PHONE_PATTERN)) {
            return "전화번호는 010-0000-0000 형식으로 입력해 주세요.";
        }

        if (updateDto.getBirthDate() != null && updateDto.getBirthDate().isAfter(LocalDate.now())) {
            return "생년월일은 오늘 이후 날짜로 설정할 수 없습니다.";
        }

        if (!isBlank(updateDto.getGender())
                && !"M".equalsIgnoreCase(updateDto.getGender())
                && !"F".equalsIgnoreCase(updateDto.getGender())) {
            return "성별 값이 올바르지 않습니다.";
        }

        return null;
    }

    private boolean isValidPassword(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
