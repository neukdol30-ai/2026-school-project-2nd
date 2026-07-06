package com.siyan1234.itproject2nd.member.member_service;

import com.siyan1234.itproject2nd.member.member_dao.PortalMemberDao;
import com.siyan1234.itproject2nd.member.member_dto.PortalMemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortalMemberService {

    private final PortalMemberDao portalMemberDao;

    @Transactional
    public int signup(PortalMemberDto portalMemberDto) {

        // 화면에서 넘어온 값 앞뒤 공백 정리
        String memberId = trim(portalMemberDto.getMemberId());
        String password = trim(portalMemberDto.getPassword());
        String passwordCheck = trim(portalMemberDto.getPasswordCheck());
        String name = trim(portalMemberDto.getName());
        String nickname = trim(portalMemberDto.getNickname());
        String email = trimToNull(portalMemberDto.getEmail());

        // 아이디 필수 검사
        if (isBlank(memberId)) {
            throw new IllegalArgumentException("아이디를 입력해주세요.");
        }

        // 비밀번호 필수 검사
        if (isBlank(password)) {
            throw new IllegalArgumentException("비밀번호를 입력해주세요.");
        }

        // 이름 필수 검사
        if (isBlank(name)) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }

        // 닉네임 필수 검사
        // DB에서 NICKNAME이 NOT NULL이라 반드시 필요함
        if (isBlank(nickname)) {
            throw new IllegalArgumentException("닉네임을 입력해주세요.");
        }

        // 비밀번호 규칙 검사
        // 8자 이상 + 영문 + 숫자 포함
        if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 8자 이상, 영문과 숫자를 포함해야 합니다.");
        }

        // 비밀번호 확인 검사
        if (!password.equals(passwordCheck)) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 이용약관 동의 검사
        if (!"Y".equals(portalMemberDto.getAgreeTermsYn())) {
            throw new IllegalArgumentException("이용약관에 동의해주세요.");
        }

        // 개인정보 동의 검사
        if (!"Y".equals(portalMemberDto.getAgreePrivacyYn())) {
            throw new IllegalArgumentException("개인정보 처리방침에 동의해주세요.");
        }

        // 아이디 중복 검사
        if (portalMemberDao.countByMemberId(memberId) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 이메일은 선택값
        // 입력했을 때만 중복 검사
        if (email != null && portalMemberDao.countByEmail(email) > 0) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 정리한 값 다시 DTO에 넣기
        portalMemberDto.setMemberId(memberId);
        portalMemberDto.setPassword(password);
        portalMemberDto.setName(name);
        portalMemberDto.setNickname(nickname);
        portalMemberDto.setEmail(email);

        // 권한은 일반 회원 USER로 고정
        portalMemberDto.setRole("USER");

        // 회원가입 INSERT 실행
        return portalMemberDao.signup(portalMemberDto);
    }

    // 아이디 중복확인 버튼에서 사용
    public boolean isDuplicateMemberId(String memberId) {

        memberId = trim(memberId);

        // 빈 아이디는 사용 불가 처리
        if (isBlank(memberId)) {
            return true;
        }

        return portalMemberDao.countByMemberId(memberId) > 0;
    }

    // null 방지 + 앞뒤 공백 제거
    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    // 빈 값 검사
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // 선택 입력값은 빈 문자열이면 null로 처리
    private String trimToNull(String value) {

        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        return value.trim();
    }
}