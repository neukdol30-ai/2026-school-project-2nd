package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.config.security.PasswordPolicy;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dao.SocialAccountDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.PendingSocialSignupDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import com.siyan1234.itproject2nd.member.dto.SocialAccountDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor // final 필드 생성자 주입 방식으로 자동 처리.
@Slf4j
public class MemberService {

    private final MemberDao memberDao; // DB 작업 담당 DAO

    private final PasswordEncoder passwordEncoder; // BCrypt 암호화 담당

    private final SocialAccountDao socialAccountDao;

    // Redis에 저장해 둔 소셜 가입 대기정보를 읽는 Service
    private final PendingSocialSignupService pendingSocialSignupService;

    private static final String PASSWORD_PATTERN = PasswordPolicy.PASSWORD_REGEX;

    //  회원가입 검증 오류 확인
    public boolean hasSignupErrors(SignupDto signupDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) { // @NotBlank, @Email -> 기본 검증 오류 있는지 확인
            return true; // 오류 O -> 회원가입 중단.
        }

        if (!signupDto.getPassword().equals(signupDto.getPasswordCheck())) { // 비밀번호 + 비밀번호 확인이 같은지 비교
            bindingResult.rejectValue("passwordCheck", "passwordMismatch", "비밀번호가 일치하지 않습니다."); // passwordCheck 필드에 오류를 추가.
            return true;
        }

        if (memberDao.findByMemberId(signupDto.getMemberId()) != null) { // 같은 ID가 DB에 있는지 확인
            bindingResult.rejectValue("memberId", "duplicateMemberId", "이미 사용 중인 아이디입니다."); // memberId 필드에 오류 추가
            return true;
        }

        if (memberDao.findByNickname(signupDto.getNickname()) != null) { // 같은 닉네임 DB에 있는지 확인
            bindingResult.rejectValue("nickname", "duplicateNickname", "이미 사용 중인 닉네임입니다."); // nickname 필드에 오류 추가
            return true; // 중복이면 회원가입 중단
        }

        if (signupDto.getEmail() != null && !signupDto.getEmail().isBlank()) {
            if (memberDao.findByEmail(signupDto.getEmail()) != null) {
                bindingResult.rejectValue("email", "duplicateEmail", "이미 사용 중인 이메일입니다.");
                return true;
            }
        }

        // 생년월일 검증(값 자체는 사용자가 만들었지만, 그 값이 유효한지 판단하는 건 이 코드)
        if (signupDto.getBirthDate() != null) {
            LocalDate today = LocalDate.now();

            if (signupDto.getBirthDate().isAfter(today)) { // (1) 미래 날짜 차단
                bindingResult.rejectValue("birthDate", "futureBirthDate", "생년월일은 오늘 이전 날짜여야 합니다.");
                return true;
            }

            LocalDate oldestAllowed = today.minusYears(120); // (2) 과거 하한선 : today.minusYears(14)와 같은 방식(자동으로 매년 기준이 밀림)
            if (signupDto.getBirthDate().isBefore(oldestAllowed)) { // 하한선보다 더 과거 날짜면 차단
                bindingResult.rejectValue("birthDate", "tooOldBirthDate", "올바른 생년월일을 입력해 주세요.");
                return true;
            }

            LocalDate fourteenYearsAgo = today.minusYears(14);

            if (signupDto.getBirthDate().isAfter(fourteenYearsAgo)) { // (3) 만 14세 미만 차단
                bindingResult.rejectValue("birthDate", "underAge", "만 14세 미만은 가입할 수 없습니다.");
                return true;
            }
        }

        if (!"Y".equals(signupDto.getAgreeTermsYn())) { // 이용약관의 동의값이 Y인지 확인
            bindingResult.rejectValue("agreeTermsYn", "requiredAgreeTerms", "이용약관에 동의해야 합니다.");
            return true;
        }

        if (!"Y".equals(signupDto.getAgreePrivacyYn())) {
            bindingResult.rejectValue("agreePrivacyYn", "requiredAgreePrivacy", "개인정보 처리방침에 동의해야 합니다.");
            return true;
        }

        return false; // 여기까지 통과하면 회원가입 검증 오류 없음.
    }

    public void signup(SignupDto signupDto) { // 실제 회원가입 저장 처리

        String encodedPassword = passwordEncoder.encode(signupDto.getPassword()); // 원본 비밀번호를 BCrypt 해시값으로 바꿈

        signupDto.setPassword(encodedPassword); // DTO의 password 값을 암호화된 비밀번호로 교체

        memberDao.signup(signupDto); // 암호화된 비밀번호가 들어 있는 DTO를 DB에 INSERT
    }

    public List<MemberDto> findAllMembers() {
        return memberDao.findAllMembers();
    }

    public MemberDto findByNo(Integer no) {
        return memberDao.findByNo(no); // DAO에 no를 넘겨 한 명의 회원 정보를 받아서 그대로 돌려줌
    }

    public void updateMember(MemberDto memberDto) {
        memberDao.updateMember(memberDto);
    }


    /**
     * 관리자 회원 단건 삭제 처리입니다.
     * member 테이블의 FK는 대부분 ON DELETE SET NULL/CASCADE로 설계되어 있어
     * 회원 삭제 시 관련 데이터는 작성자 없음 또는 연동 정보 삭제로 정리됩니다.
     */
    public int deleteMember(Integer no) {
        if (no == null) {
            return 0;
        }

        return memberDao.deleteMember(no);
    }

    /**
     * 관리자 회원 다중 삭제 처리입니다.
     * 현재 로그인한 관리자 계정은 실수로 삭제되지 않도록 제외합니다.
     */
    public int deleteMembers(List<Integer> memberNoList, Integer loginAdminNo) {
        if (memberNoList == null || memberNoList.isEmpty()) {
            return 0;
        }

        int deletedCount = 0;

        for (Integer memberNo : memberNoList) {
            if (memberNo == null) {
                continue;
            }

            if (loginAdminNo != null && loginAdminNo.equals(memberNo)) {
                continue;
            }

            deletedCount += memberDao.deleteMember(memberNo);
        }

        return deletedCount;
    }

    // 아이디 중복 여부 확인 (실시간 체크) / DB에 같은 아이디가 있으면 true(중복), 없으면 false(사용 가능)
    public boolean isMemberIdDuplicate(String memberId) {
        // findByMemberId는 이미 있는 메서드. 조회 결과 null 아니면 = 그 아이디 이미 존재 = 중복
        return memberDao.findByMemberId(memberId) != null;
    }

    // 닉네임 중복 여부 확인 (실시간 체크) / DB에 같은 닉네임이 있으면 true(중복), 없으면 false(사용 가능)
    public boolean isNicknameDuplicate(String nickname) {
        // 조회 결과 null 아니면 닉네임 이미 존재 -> 중복
        return memberDao.findByNickname(nickname) != null;
    }

    // 아이디, 비밀번호 찾기 재작업

    // 아이디 찾기 : 인증된 이메일로 회원 한 명 조회 -> memberId를 알아낸다.
    public MemberDto findByEmail(String email) {
        return memberDao.findByEmail(email);
    }

    // 비밀번호 찾기 : 입력한 아이디 + 이메일이 같은 회원인지 본인 확인
    public MemberDto findByMemberIdAndEmail(String memberId, String email) {
        return memberDao.findByMemberIdAndEmail(memberId, email);
    }

    // 비밀번호 규칙 검증
    public boolean isPasswordValid(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
        // matches()는 String의 일부가 아니라 "문자열 전체"가 패턴과 일치해야 true.
    }

    // 비밀번호 재설정 : 규칙 검증 -> BCrypt 해싱 -> UPDATE
    // false 반환 = 규칙 위반이라 저장 자체를 안 한 것.(Controller가 이 값으로 오류 메시지 결정)
    public boolean updatePasswordByEmail(String email, String newPassword) {
        if (!isPasswordValid(newPassword)) { // 규칙에 안 맞는 비밀번호?
            return false; // 참이면 실행 : DB 저장 시도조차 안 하고 즉시 반환
        } // 규칙 통과

        String encodedPassword = passwordEncoder.encode(newPassword); // 원본 비밀번호 -> BCrypt 해시로 변환
        return memberDao.updatePasswordByEmail(email, encodedPassword) > 0;
        // updatePasswordByEmail(Dao)의 반환값은 "영향받은 행 수"(int). 1 이상이면 실제로 수정된 행 있다는 뜻 -> true / 0이면 false.

    }

    // 현재 로그인한 회원의 약관 동의 상태를 DB에서 Y/Y로 변경
    public boolean updateAgreement(Integer no) {

        if (no == null) {
            return false;
        }

        // DAO가 돌려준 수정 행 수가 1 이상이면 실제 DB 수정에 성공한 것
        return memberDao.updateAgreement(no) > 0;
    }

    @Transactional
    public MemberDto completeSocialSignup(String pendingToken) {
        // 1단계 : 열쇠 자체가 없는 경우 차단
        if (pendingToken == null || pendingToken.isBlank()) {
            log.info("소셜 가입 확정 실패 : pendingToken이 없습니다.");
            return null;
        }

        // 2단계 : Redis에서 대기정보 꺼내기
        // find()는 Redis에 값이 있으면 DTO를, 없으면 null을 돌려줌
        PendingSocialSignupDto pending = pendingSocialSignupService.find(pendingToken);

        if (pending == null) {
            log.info("소셜 가입 확정 실패 : 대기정보가 없습니다(10분 만료 추정).");
            return null;
        }

        String provider = pending.getProvider();
        String providerId = pending.getProviderId();

        // 3단계 : 필수값 방어
        if (provider == null || provider.isBlank()
                || providerId == null || providerId.isBlank()) {
            log.error("소셜 가입 확정 실패 : provider 또는 providerId가 비어 있습니다.");
            return null;
        }

        // 4단계 : 이미 가입된 계정인지 확인 (중복 제출 방어)
        // 사용자가 동의 버튼 두 번 누르거나 새로고침으로 재전송할 때를 대비
        SocialAccountDto existing =
                socialAccountDao.findByProviderAndProviderId(provider, providerId);

        if (existing != null) {
            log.info("이미 가입 완료된 소셜 계정입니다. provider={}", provider);

            return memberDao.findByNo(existing.getMemberNo());
        }

        // 5단계 : 로그인 아이디 만들기
        String memberId = provider + "_" + providerId;

        // 방어 검사 : social_account에 없는데 member_id는 이미 존재한다면 데이터가 어긋난 상태
        if (memberDao.findByMemberId(memberId) != null) {
            log.error("소셜 가입 확정 실패 : member_id가 이미 존재합니다. memberId={}", memberId);
            return null;
        }

        // 6단계 : 저장할 회원 정보 조립
        // Redis에서 꺼낸 값으로 새 객체를 만듦. 로그인 정보(CustomUserDetails) 안의 MemberDto 재사용 X.
        MemberDto memberDto = new MemberDto();

        memberDto.setMemberId(memberId); // 5단계에서 만든 로그인 아이디

        memberDto.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        memberDto.setName(cutText(pending.getName(), 50));

        memberDto.setNickname(resolveNickname(pending.getNickname(), provider));

        memberDto.setEmail(resolveEmail(pending.getEmail()));

        // 7단계 : member 테이블에 INSERT
        int insertedMemberCount = memberDao.insertSocialMember(memberDto);

        if (insertedMemberCount != 1) {
            log.error("소셜 회원 INSERT 실패 memberId={}", memberId);
            return null;
        }

        // 8단계 : 방금 저장된 회원번호(no) 회수
        MemberDto savedMember = memberDao.findByMemberId(memberId);

        if (savedMember == null || savedMember.getNo() == null) {
            log.error("소셜 회원 저장 후 재조회 실패 memberId={}", memberId);
            // RuntimeException을 던져야 @Transactional이 앞의 INSERT를 롤백한다.
            throw new IllegalStateException("소셜 회원 저장 후 회원번호를 찾지 못했습니다.");
        }

        // 9단계 : social_account 테이블에 연동 정보 INSERT

        // SocialAccountDto는 @Builder가 있어 빌더 방식으로 조립
        SocialAccountDto socialAccountDto = SocialAccountDto.builder()
                .memberNo(savedMember.getNo())
                .provider(provider)
                .providerId(providerId)
                .build();

        int insertedSocialCount = socialAccountDao.insertSocialAccount(socialAccountDto);

        if (insertedSocialCount != 1) {
            log.error("social_account INSERT 실패 memberNo={}", savedMember.getNo());
            // 여기서 예외를 던지면 7단계의 member INSERT까지 함께 취소된다.
            throw new IllegalStateException("소셜 연동 정보 저장에 실패했습니다.");
        }

        log.info("소셜 회원가입 확정 완료 provider={}, memberNo={}", provider, savedMember.getNo());

        // Redis 임시정보 삭제는 여기서 하지 않음.
        return savedMember; // 회원번호까지 채워진 정식 회원 정보를 Controller에 돌려줌
    }

    // 소셜에서 받은 닉네임을 member.nickname 컬럼에 넣어도 안전한 값으로 바꿈
    private String resolveNickname(String socialNickname, String provider) {

        // (가) NULL, 공백 방어
        String base = socialNickname;

        if (base == null || base.isBlank()) {
            base = "kakao".equals(provider) ? "카카오사용자" : "네이버사용자";
        }

        base = base.trim();

        // (나) 길이 방어

        // 컬럼은 50글자까지 / 뒤에 "_숫자"를 붙일 자리를 남겨 40글자로 자름
        base = cutText(base, 40);

        // (다) 중복 방어

        String candidate = base; // 원본 그대로
        int suffix = 1; // 겹칠 때 뒤에 붙일 번호

        // 닉네임을 쓰는 회원이 이미 있다
        while (memberDao.findByNickname(candidate) != null) {

            candidate = base + "_" + suffix;
            suffix++;

            if (suffix > 100) {
                candidate = base + "_" + System.currentTimeMillis();
                break;
            }
        }

        return candidate; // 저장해도 안전한 닉네임
    }

    // 소셜은 이메일을 member.email 컬럼에 넣어도 안전한 값으로 바꿈

    private String resolveEmail(String socialEmail) {

        if (socialEmail == null || socialEmail.isBlank()) {
            return null;
        }

        String email = cutText(socialEmail.trim(), 100);

        // 대기 10분 사이에 다른 사람이 같은 이메일로 일반 가입했을 수도 있음. 다시 확인
        if (memberDao.findByEmail(email) != null) {
            log.warn("소셜 이메일이 이미 사용 중이라 이메일 없이 가입합니다.");
            return null; // 이메일만 비우고 가입은 정상 진행 (UNIQUE 위반 회피)
        }

        return email;
    }

    // 문자열을 지정한 글자 수까지만 남기고 잘라냄
    private String cutText(String text, int maxLength) {

        if (text == null) {
            return null;
        }

        if (text.length() <= maxLength) {
            return text;
        }

        String cut = text.substring(0, maxLength);

        if (!cut.isEmpty() && Character.isHighSurrogate(cut.charAt(cut.length() -1))) {
            cut = cut.substring(0, cut.length() -1);
        }

        return cut;
    }

}
