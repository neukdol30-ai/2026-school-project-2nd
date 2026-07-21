package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.config.security.PasswordPolicy;
import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor // final 필드 생성자 주입 방식으로 자동 처리.
public class MemberService {

    private final MemberDao memberDao; // DB 작업 담당 DAO

    private final PasswordEncoder passwordEncoder; // BCrypt 암호화 담당

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
        if (signupDto.getBirthDate()!=null) {
            LocalDate today = LocalDate.now();

            if (signupDto.getBirthDate().isAfter(today)) { // (1) 미래 날짜 차단
                bindingResult.rejectValue("birthDate", "futureBirthDate", "생년월일은 오늘 이전 날짜여야 합니다.");
                return true;
            }

            LocalDate oldestAllowed = today.minusYears(120); // (2) 과거 하한선 : today.minusYears(14)와 같은 방식(자동으로 매년 기준이 밀림)
            if (signupDto.getBirthDate().isBefore(oldestAllowed)) { // 하한선보다 더 과거 날짜면 차단
                bindingResult.rejectValue("birthDate", "tooOldBirthDate", "올바른 생년월일을 입력해 주세요,");
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

        memberDao.signup(signupDto); // 암호화된 비밀번호가 들어 있는 DTO를 DB에 INSERTㅊ
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
}
