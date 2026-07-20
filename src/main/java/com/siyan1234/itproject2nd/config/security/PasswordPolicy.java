package com.siyan1234.itproject2nd.config.security;

/*
 * 비밀번호 처리 규칙을 한 곳에서 관리하는 상수 클래스
 * 같은 규칙(8~20자 + 대문자 + 소문자 + 숫자) SignupDto, MemberService 두 곳에 나눠서 있음.
 * 한 곳에서만 고치면 회원가입은 통과하는데 비밀번호 재설정은 거부되는 식의 불일치 발생. -> 여기만 고치면 두 곳 동시에 바뀌도록.
 * signup.js / reset-password.js 화면 검사는 자바 상수를 읽을 수 없어 따로 적혀 있음. (규칙 변경 시 JS 2개 파일 함께 수정해야 함 -> 한계)
 * */
public final class PasswordPolicy {

    public static final String PASSWORD_REGEX = "(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8, 20}";

    public static final String PASSWORD_MESSAGE = "비밀번호는 영문 대문자·소문자·숫자를 모두 포함해 8~20자로 입력하세요.";

    private PasswordPolicy() {
    }
}
