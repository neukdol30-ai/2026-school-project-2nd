package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dao.SocialAccountDao;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
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
    private final PasswordEncoder passwordEncoder; // BCrypt 해싱 도구 (기존 설정에 이미 빈 등록되어 있음)

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // super.loadUser(...) = 부모 클래스(DefaultOAuth2UserService)의 원래 기능을 그대로 실행.
        OAuth2User oAuth2User = super.loadUser(userRequest); // 이 코드가 실제 카카오/네이버 서버에 HTTP 요청 보내서 사용자 정보 JSON 받아온다.

        // getAttributes() = 방금 받아온 JSON을 Map으로 변환한 것
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // getRegistrationId() = application-secret.yaml의 registration 아래 적은 이름("kakao" / "naver")
        String provider = userRequest.getClientRegistration().getRegistrationId();

        log.info("소셜 로그인 provider = {}", provider);
        log.info("소셜 응답 원본 = {}", attributes); // 실패 시 여기 로그 보면 JSON 구조 정확히 알 수 있음.

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
        String email = socialUserInfo.getEmail(); // null일 수 있음.

        // 2단계 : 이 소셜 계정이 social_account 테이블에 이미 연결되어 있는지 확인.
        SocialAccountDto socialAccount =
                socialAccountDao.findByProviderAndProviderId(provider, providerId); // 두 값을 조건으로 기존 연결 정보 1건 조회

        // socialAccount가 null이 아니면 이전 로그인에서 이미 우리 회원가 연결된 소셜 계정.
        if (socialAccount != null) {
            // social_account.member_no 저장된 회원 번호로 member 테이블의 실제 회원 정보 조회.
            MemberDto memberDto = memberDao.findByNo(socialAccount.getMemberNo()); // 연결된 회원 번호에 해당하는 MemberDto 가져옴.

            // 1. 방어 검사 : social_account 연결 정보는 있는데 member 회원 정보가 없는 경우
            if (memberDto == null) {
                // 사용자 화면이 아닌 서버 콘솔에 문제 상황과 대상 회원 번호 기록
                log.warn(
                        "소셜 연결 정보는 있지만 회원 정보가 없음 memberNo={}",
                        socialAccount.getMemberNo());
                // OAuth2AuthenticationException을 던지면 소셜 인증은 실패로 종료
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                ERROR_MEMBER_NOT_FOUND,
                                "회원 정보를 찾을 수 없습니다. 관리자에게 문의해 주세요.",
                                null));
            } // 회원 정보 없음 검사 종료

            // 2. 보안 검사 : 관리자가 정지한 회원인지 확인
            // OAuth2DetailsService에서 직접 memberDto.isBanned()를 확인
            if (memberDto.isBanned()) { // banYn 값이 "Y" -> true 반환.
                // 정지 회원 로그인 시도를 회원 번호와 함께 서버 로그에 기록
                log.warn(
                        "정지 회원의 소셜 로그인 차단 memberNo={}",
                        memberDto.getNo());

                // 소셜 로그인 실패 예외 발생시켜 이후 로그인 성공 처리 막음
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                ERROR_MEMBER_BANNED,
                                memberDto.displayBanReason(), // 사유 있으면 정지 사유, 없으면 기본 정지 문구 반환
                                null));
            } // 정지 회원 검사 종료

            // 회원 정보가 존재하고 정지 상태도 아니면 정상적인 기존 소셜 회원.
            return new CustomUserDetails(
                    memberDto, // 우리 member 테이블에서 조회한 로그인 회원 정보.
                    attributes); // 카카오, 네이버가 반환한 사용자 정보 Map 객체
        }

        // 3단계 : 이메일이 같은 기존 회원이 있으면 연동하지 않고 로그인 차단.
        // 우리 일반 회원가입은 이메일 소유 인증 X -> 이메일만 믿고 자동 연동하면, 공격자가 선점형 계정 탈취
        if (email != null && !email.isBlank()) { // 이메일 동의 거부하면 null 올 수 있음.

            MemberDto duplicatedMember = memberDao.findByEmail(email); // 있으면 MemberDto, 없으면 null

            if (duplicatedMember != null) { // 이메일이 겹치는 회원이 이미 있음.

                log.warn("소셜 로그인 이메일 중복 차단 provider={}, email={}", provider, email); // 개발자용 기록

                // OAuth2Error(오류 코드, 화면에 보일 설명, 참고 URL) 순서로 담는다.
                // 이 예외 던지면 Security가 OAuth2LoginFailureHandler 호출
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(
                                ERROR_EMAIL_ALREADY_REGISTERED, // 화이트 리스트에 등록된 코드
                                "이미 가입된 이메일입니다. 기존에 사용하던 로그인 방법으로 로그인해 주세요.",
                                null)); // 참고 URL은 쓰지 않음
            }
        }

        // 4단계 : 여기까지 왔으면 확실한 신규 회원. -> member 테이블에 INSERT (이제 연동 분기가 없으므로 여기서 만듦)
        MemberDto memberDto = new MemberDto();

        // (가) member_id 생성. UNIQUE, CustomUserDetails.getUsername()이 이 값을 반환.
        String memberId = provider + "_" + providerId; // 예 : "kakao_3948573"

        // (나) 소셜 회원은 비밀번호로 로그인 X -> 컬럼 비우면 빈 비밀번호 시도 위험 있음. -> 무작위 문자열 BCrypt 해싱
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        // (다) nickname은 NOT NULL, UNIQUE라 반드시 겹치지 않아야 함.
        String nickname = generateUniqueNickname(socialUserInfo.getNickname(), provider, providerId);

        // (라) MemberDto 조립. @Setter가 있어 set으로 채움.
        memberDto.setMemberId(memberId);
        memberDto.setPassword(randomPassword);
        memberDto.setName(socialUserInfo.getName()); // null 가능 (컬럼이 nullable)
        memberDto.setNickname(nickname);
        memberDto.setEmail(email); // null 가능

        memberDao.insertSocialMember(memberDto); // -> MemberMapper.xml의 <insert id="insertSocialMember">

        // INSERT 직후 memberDto의 no는 아직 비어 있음.
        // member_id가 UNIQUE이므로 그 값으로 다시 조회하면 no가 채워진 완전한 객체 얻음.
        memberDto = memberDao.findByMemberId(memberId);

        log.info("소셜 신규 회원 생성 memberId = {}, nickname = {}", memberId, nickname);

        // 5단계 : social_account에 연결 정보 저장
        SocialAccountDto newSocialAccount = SocialAccountDto.builder() // @BUilder 있어서 .필드(값) 조립 가능
                .memberNo(memberDto.getNo())
                .provider(provider)
                .providerId(providerId)
                .build(); // build()를 호출해야 실제 객체가 만들어진다.

        socialAccountDao.insertSocialAccount(newSocialAccount);

        // @AuthenticationPrincipal CustomUserDetails로 어디서든 꺼내 쓸 수 있음.
        return new CustomUserDetails(memberDto, attributes);
    }

    // 닉네임 중복 회피
    private String generateUniqueNickname(String rawNickname, String provider, String providerId) {
        // (1) 기준 닉네임 정하기
        // 카카오/네이버에서 닉네임 동의 거부당하면 rawNickname이 null로. 그 때는 절대 겹치지 않는 대체값(예: "kakao_39485")을 사용
        String base;

        if (rawNickname == null || rawNickname.isBlank()) {
            // providerId가 길 수 있으니 앞 5글자만 사용. Math.min으로 길이 초과 예외 막음.
            String shortId = providerId.substring(0, Math.min(5, providerId.length()));
            base = provider + "_" + shortId; // 예: "kakao_39485"
        } else {
            base = rawNickname.trim(); // 앞뒤 공백 제거
        }

        // (2) 길이 제한
        // 컬럼 여유 있어도 화면 표시 안 깨지게 20자로 자르기 + 뒤에 숫자 붙일 여유
        if (base.length() > 20) {
            base = base.substring(0, 20); // 0번째부터 19번째 글자까지 (20번째 미포함)
        }

        // (3) 중복일 때 뒤에 숫자 붙이기
        String candidate = base; // 첫 시도는 숫자 없이 원본
        int suffix = 1; // 붙일 숫자. 1부터 시작

        while (memberDao.findByNickname(candidate) != null) {
            candidate = base + suffix; // 문자열 + 숫자 => "홍길동" + 1 => "홍길동1"
            suffix = suffix + 1;

            // 무한 루프 방지.
            if (suffix > 10000) {
                candidate = base + UUID.randomUUID().toString().substring(0, 8);
                break;
            }
        }
        // 흐름 예시:
        //   DB에 "홍길동" 없음        → 1회차 조건 거짓 → 반복 안 함 → "홍길동" 반환
        //   DB에 "홍길동" 있음        → 1회차 참 → candidate="홍길동1", suffix=2
        //   DB에 "홍길동1"도 있음     → 2회차 참 → candidate="홍길동2", suffix=3
        //   DB에 "홍길동2" 없음       → 3회차 거짓 → "홍길동2" 반환
        return candidate;
    }

}















