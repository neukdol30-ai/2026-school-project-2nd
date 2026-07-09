package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dao.MemberDao;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import com.siyan1234.itproject2nd.member.dto.SignupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import java.util.List;

@Service
@RequiredArgsConstructor // final 필드 생성자 주입 방식으로 자동 처리.
public class MemberService {

    private final MemberDao memberDao; // DB 작업 담당 DAO

    private final PasswordEncoder passwordEncoder; // BCrypt 암호화 담당

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

        if (signupDto.getEmail() != null && !signupDto.getEmail().isBlank()) {
            if (memberDao.findByEmail(signupDto.getEmail()) != null) {
                bindingResult.rejectValue("email", "duplicateEmail", "이미 사용 중인 이메일입니다.");
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
}
