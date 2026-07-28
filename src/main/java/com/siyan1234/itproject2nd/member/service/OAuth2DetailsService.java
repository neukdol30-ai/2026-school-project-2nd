package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dao.SocialAccountDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.PendingSocialSignupDto;
import com.siyan1234.itproject2nd.member.dto.SocialAccountDto;
import com.siyan1234.itproject2nd.member.social.KakaoUserInfo;
import com.siyan1234.itproject2nd.member.social.NaverUserInfo;
import com.siyan1234.itproject2nd.member.social.SocialUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.core.OAuth2Error; // 오류 코드 + 안내문구

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2DetailsService extends DefaultOAuth2UserService {

    // 오류 코드 상수 3개. 실패 핸들러의 화이트 리스트가 이 값을 그대로 참조.
    public static final String ERROR_EMAIL_ALREADY_REGISTERED = "email_already_registered"; // STEP 1
    public static final String ERROR_MEMBER_BANNED = "member_banned"; // STEP 2에서 사용
    public static final String ERROR_MEMBER_NOT_FOUND = "member_not_found"; // STEP 2에서 사용

    private final MemberDao memberDao; // member 테이블 접근
    private final SocialAccountDao socialAccountDao; // social_account 테이블 접근
    private final PendingSocialSignupService pendingSocialSignupService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // super.loadUser(...) = 부모 클래스(DefaultOAuth2UserService)의 원래 기능을 그대로 실행.
        OAuth2User oAuth2User = super.loadUser(userRequest); // 이 코드가 실제 카카오/네이버 서버에 HTTP 요청 보내서 사용자 정보 JSON 받아온다.

        // getAttributes() = 방금 받아온 JSON을 Map으로 변환한 것
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // getRegistrationId() = application-secret.yaml의 registration 아래 적은 이름("kakao" / "naver")
        String provider = userRequest.getClientRegistration().getRegistrationId();

        log.info("소셜 로그인 provider = {}", provider);

        // 실명과 이메일이 포함된 응답 전체를 INFO로 콘솔에 남기지 않는다. -> DEBUG 레벨로 낮춤. 기본 설정에서는 DEBUG가 화면에 안 찍힘
        log.debug("소셜 응답 원본 = {}", attributes);

        // 1단계 : provider에 맞는 해석기 고르기
        SocialUserInfo socialUserInfo;

        if (provider.equals("kakao")) {
            socialUserInfo = new KakaoUserInfo(attributes);
        } else if (provider.equals("naver")) {
            socialUserInfo = new NaverUserInfo(attributes);
        } else {
            // yaml에 없는 provider가 들어올 일은 없지만, 원인 불명 NullPointerException을 막기 위해.
            throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다 : " + provider);
        }

        String providerId = socialUserInfo.getProviderId(); // 예 : "3948573"

        // 개인정보(이름, 이메일) 없이 provider와 회원번호만 남기는 최소 로그 / 탈퇴, 연결해제 테스트 판정에는 이 두 값이면 충분
        log.info("소셜 로그인 식별자 provider={}, providerId={}", provider, providerId);

        String email = socialUserInfo.getEmail(); // null일 수 있음.

        // 2단계 : 이 provider+providerId 조합으로 이미 연결된 계정이 있는지 조회
        SocialAccountDto socialAccount = socialAccountDao.findByProviderAndProviderId(provider, providerId);


        // socialAccount가 null이 아니면 이전 로그인에서 이미 우리 회원가 연결된 소셜 계정.
        if (socialAccount != null) {
            // social_account.member_no 저장된 회원 번호로 member 테이블의 실제 회원 정보 조회.
            MemberDto memberDto = memberDao.findByNo(socialAccount.getMemberNo()); // 연결된 회원 번호에 해당하는 MemberDto 가져옴.

            // 1. 방어 검사 : social_account 연결 정보는 있는데 member 회원 정보가 없는 경우
            if (memberDto == null) {
                log.warn("소셜 연결 정보는 있지만 회원 정보가 없음 memberNo={}", socialAccount.getMemberNo());
                throw new OAuth2AuthenticationException(new OAuth2Error(
                        ERROR_MEMBER_NOT_FOUND, "회원 정보를 찾을 수 없습니다. 관리자에게 문의해 주세요.", null));
            }

            // 2. 보안 검사 : 관리자가 정지한 회원인지 확인
            // OAuth2DetailsService에서 직접 memberDto.isBanned()를 확인
            if (memberDto.isBanned()) {
                log.warn("정지 회원의 소셜 로그인 차단 memberNo={}", memberDto.getNo());
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(ERROR_MEMBER_BANNED, memberDto.displayBanReason(), null));
            }

            return new CustomUserDetails(memberDto, attributes);
        }


        // 3단계 : 이메일이 같은 기존 회원이 있으면 연동하지 않고 로그인 차단.
        // 우리 일반 회원가입은 이메일 소유 인증 X -> 이메일만 믿고 자동 연동하면, 공격자가 선점형 계정 탈취
        if (email != null && !email.isBlank()) { // 이메일 동의 거부하면 null 올 수 있음.

            MemberDto duplicatedMember = memberDao.findByEmail(email); // 있으면 MemberDto, 없으면 null

            if (duplicatedMember != null) {
                log.warn("소셜 로그인 이메일 중복 차단 provider={}, email={}", provider, email); // 개발자용 기록

                // OAuth2Error(오류 코드, 화면에 보일 설명, 참고 URL) 순서로 담는다.
                // 이 예외 던지면 Security가 OAuth2LoginFailureHandler 호출
                throw new OAuth2AuthenticationException(new OAuth2Error(ERROR_EMAIL_ALREADY_REGISTERED, // 화이트 리스트에 등록된 코드
                        "이미 가입된 이메일입니다. 기존에 사용하던 로그인 방법으로 로그인해 주세요.", null)); // 참고 URL은 쓰지 않음
            }
        }

        // 4단계 : 신규 소셜 사용자의 최소 정보를 Redis 가입 대기 영역에 저장 / 아직 member 테이블에 INSERT하지 않음
        PendingSocialSignupDto pendingSignup = PendingSocialSignupDto.builder()
                .provider(provider)
                .providerId(providerId)
                .name(socialUserInfo.getName())
                .nickname(socialUserInfo.getNickname())
                .email(email)
                .build();

        String pendingSocialToken = pendingSocialSignupService.save(pendingSignup);

        MemberDto pendingMemberDto = createPendingMemberDto(provider, providerId);

        log.info("신규 소셜 사용자를 약관 동의 대기 상태로 전환 provider={}", provider);

        return new CustomUserDetails(pendingMemberDto, Map.of(), pendingSocialToken);
    }

        private MemberDto createPendingMemberDto (String provider, String providerId) {

            MemberDto pendingMemberDto = new MemberDto();

            pendingMemberDto.setMemberId(provider + "_" + providerId);

            pendingMemberDto.setPassword("PENDING_SOCIAL_SIGNUP");

            pendingMemberDto.setNickname(provider + "_pending");

            pendingMemberDto.setRole("USER");
            pendingMemberDto.setAgreeTermsYn("N");
            pendingMemberDto.setAgreePrivacyYn("N");

            return pendingMemberDto;
        }
    }