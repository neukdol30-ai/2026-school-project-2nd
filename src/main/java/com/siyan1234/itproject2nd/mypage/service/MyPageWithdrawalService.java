package com.siyan1234.itproject2nd.mypage.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dao.SocialAccountDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SocialAccountDto;
import com.siyan1234.itproject2nd.member.service.KakaoUnlinkService;
import com.siyan1234.itproject2nd.mypage.dao.MyPageDao;
import com.siyan1234.itproject2nd.mypage.dto.MyPageActionResponseDto;
import com.siyan1234.itproject2nd.mypage.dto.MyPageWithdrawDto;
import com.siyan1234.itproject2nd.mypage.support.MyPageMessages;
import com.siyan1234.itproject2nd.mypage.support.MyPagePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 마이페이지 회원 탈퇴 검증, 소셜 연결 해제, 회원 삭제를 담당합니다. */
@Service
@RequiredArgsConstructor
public class MyPageWithdrawalService {

    private final MyPageDao myPageDao;
    private final MemberDao memberDao;
    private final SocialAccountDao socialAccountDao;
    private final KakaoUnlinkService kakaoUnlinkService;
    private final MyPageQueryService myPageQueryService;
    private final PasswordEncoder passwordEncoder;

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

        /*
         * 회원 삭제 전에 카카오 연결을 해제해야 provider_id를 잃지 않습니다.
         * 외부 연결 해제가 실패하면 로컬 삭제도 중단해 재시도 가능한 상태를 유지합니다.
         */
        if (!unlinkConnectedKakaoAccounts(memberNo)) {
            return MyPageActionResponseDto.fail(MyPageMessages.KAKAO_UNLINK_FAILED);
        }

        int deletedCount = myPageDao.deleteMe(memberNo);
        if (deletedCount < 1) {
            return MyPageActionResponseDto.fail(MyPageMessages.WITHDRAW_FAILED);
        }

        // 실제 SecurityContext와 HTTP 세션 정리는 성공 응답을 받은 Controller가 수행합니다.
        return MyPageActionResponseDto.successWithRedirect(MyPageMessages.WITHDRAW_COMPLETED, "/");
    }

    /** 현재 회원의 카카오 연결만 외부 API로 해제하며 다른 소셜 제공자는 그대로 둡니다. */
    private boolean unlinkConnectedKakaoAccounts(Integer memberNo) {
        List<SocialAccountDto> socialAccounts = socialAccountDao.findAllByMemberNo(memberNo);
        if (socialAccounts == null || socialAccounts.isEmpty()) {
            return true;
        }

        for (SocialAccountDto socialAccount : socialAccounts) {
            if (socialAccount == null || !"kakao".equalsIgnoreCase(socialAccount.getProvider())) {
                continue;
            }

            if (!kakaoUnlinkService.unlinkByAdminKey(socialAccount.getProviderId())) {
                return false;
            }
        }

        return true;
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

    private String validateWithdraw(Integer memberNo, MemberDto member, MyPageWithdrawDto withdrawDto) {
        if (MyPagePolicy.isAdminRole(member.getRole())) {
            return MyPageMessages.ADMIN_WITHDRAW_NOT_ALLOWED;
        }

        if (withdrawDto == null || !MyPagePolicy.isWithdrawConfirmText(withdrawDto.getConfirmText())) {
            return MyPageMessages.WITHDRAW_CONFIRM_REQUIRED;
        }

        if (myPageQueryService.isSocialLoginUser(memberNo)) {
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
}
