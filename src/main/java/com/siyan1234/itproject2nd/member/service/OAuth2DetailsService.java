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

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class OAuth2DetailsService extends DefaultOAuth2UserService {

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

        // 2단계 : 이 소셜 계정이 이미 연결되어 있는지 (두 번째 로그인 이후)
        SocialAccountDto socialAccount = socialAccountDao.findByProviderAndProviderId(provider, providerId);

        if (socialAccount != null) {
            // 이미 연결된 계정 -> 그 member를 그대로 꺼내 로그인. 새로 만들지 않음.
            MemberDto memberDto = memberDao.findByNo(socialAccount.getMemberNo());
            log.info("기존 소셜 회원 로그인 memberNo = {}", socialAccount.getMemberNo());
            return new CustomUserDetails(memberDto, attributes);
        }

        // 3단계 : 처음 들어온 소셜 계정. 기존 회원과 이메일로 연동할 수 있나?
        // 같은 사람이 이미 일반 회원가입을 했거나 다른 소셜로 가입했다면, 회원을 새로 만들지 않고 그 회원에 소셜 계정만 "추가 연결"
        MemberDto memberDto = null;

        if (email != null && !email.isBlank()) {
            memberDto = memberDao.findByEmail(email); // 있으면 MemberDto, 없으면 null
        }

        if (memberDto == null) {
            // 4단계 : 정말 신규 회원 -> member 테이블에 INSERT

            // (가) member_id 생성 (UNIQUE, CustomUserDetails.getUsername에 이 값을 반환
            // Security 내부에서 username이 null이면 문제 생김. 반드시 값 필요. 절대 겹치지 않게 (예: "kakao_3948573")
            String memberId = provider + "_" + providerId;

            // (나) password 생성 / 소셜 회원은 비밀번호 로그인 X, 하지만 컬럼 비워 두면 누군가 빈 비밀번호로 로그인 시도할 수도.
            // -> 무작위 문자열을 BCrypt로 해싱해서 넣는다.
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

            // (다) nickname 만들기. member.nickname은 NOT NULL + UNIQUE. 반드시 겹치지 않아야 함.
            String nickname = generateUniqueNickname(socialUserInfo.getNickname(), provider, providerId);

            // (라) MemberDto 조립. @Setter -> set으로 채움
            memberDto = new MemberDto();
            memberDto.setMemberId(memberId);
            memberDto.setPassword(randomPassword);
            memberDto.setName(socialUserInfo.getName()); // null 가능 (zjffjadl nullable이라 OK)
            memberDto.setNickname(nickname);
            memberDto.setEmail(email); // null 가능

            memberDao.insertSocialMember(memberDto); // -> MemberMapper.xml의 <insert id="insertSocialMember">

            // INSERT 직후 memberDto의 no는 아직 비어 있음.(DB가 부여한 값이라 자바는 모름)
            // member_id가 UNIQUE이므로 그 값으로 다시 조회하면 no가 채워진 완전한 객체 얻음.
            memberDto = memberDao.findByMemberId(memberId);

            log.info("소셜 신규 회원 생성 memberId = {}, nickname = {}", memberId, nickname);
        } else {
            log.info("이메일 일치 -> 기존 회원에 소셜 계정 연동 memberNo = {}", memberDto.getNo());
        }

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















