package com.siyan1234.itproject2nd.member.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MailService {

    // 의존 객체 (Bean으로 이미 등록)
    private final JavaMailSender mailSender;

    private final StringRedisTemplate redisTemplate;

    // secret.yaml에서 읽어오는 값
    @Value("${spring.mail.username}") // @Value : yaml 설정 파일 값을 필드에 직접 넣음.
    private String fromAddress; // 이 필드에 final 붙이면 값 안 들어오고 null. -> final 안 붙임.

    private static final int AUTH_CODE_LENGTH = 6; // 인증번호 자릿수
    private static final int AUTH_CODE_BOUND = (int) Math.pow(10, AUTH_CODE_LENGTH); // 6자리 -> 1,000,000
    private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(3); // 인증번호 유효시간
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10); // "인증 완료" 상태 유지시간

    // 재발송 쿨타임 + 오입력 제한
    private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60); // 발송 후 재발송까지 강제 대기시간(60초)
    // 60은 find-id.js / find-password.js의 COOLDOWN_SECONDS 상수와 반드시 같은 값이어야 함.(자동 동기화 X)
    private static final int MAX_FAIL_COUNT = 5; // 인증번호 오입력 허용 최대 횟수

    private final SecureRandom secureRandom = new SecureRandom(); // 난수 생성기, 남이 맞히면 안 되는 값엔 반드시 써야 함.

    // enum : 이 목적으로만 쓰이는 정해진 값들 묶음. -> 아이디 찾기 + 비밀번호 찾기. 두 가지 목적만 존재하도록 제한.(두 기능의 인증번호가 Redis 키 레벨에서 안 섞이게)
    public enum MailPurpose {
        FIND_ID("findId"), // 아이디 찾기용 Redis 키 이름표
        RESET_PW("resetPw"); // 비밀번호 찾기(재설정)용

        private final String keyName; // Redis 키 문자열 조립에 쓰이는 값

        MailPurpose(String keyName) { // enum 전용 생성자(외부에서 new로 못 만들고 위 두 값만 존재)
            this.keyName = keyName;
        }

        public String getKeyName() {
            return keyName; // 이 값을 꺼내 쓰는 곳 -> buildAuthKey 등 아래 private 메서드들
        }
    }

    // 쿨타임 초과 시 던지는 전용 예외
    // static class로 MailService 안에 중첩시킨 이유 : 이 예외는 MailService 밖에서 독립적으로 쓸 일 없음. (별도 파일 X, 관련 클래스에 묶어둠)
    // RuntimeException 상속 : "체크 예외" 아니라서 메서드 시그니처에 throws 선언 안 해도 됨.
    public static class MailCooldownException extends RuntimeException {
        private final long remainingSeconds; // 몇 초 더 기다려야 하는지 값을 들고 다님

        public MailCooldownException(long remainingSeconds) {
            super(remainingSeconds + "초 후 다시 시도해 주세요."); // 부모(RuntimeException)의 메시지 필드에 저장
            this.remainingSeconds = remainingSeconds; // 이 객체 자신의 필드에도 따로 저장
        }

        public long getRemainingSeconds() { // Controller가 이 값을 꺼내서 사용자에게 보여줄 문구 만듦
            return remainingSeconds;
        }
    }

    // 인증번호 생성 -> 이메일로 보내고, Redis에 3분짜리로 저장.
    // 메일 발송 실패하면 방금 저장한 인증번호 즉시 지움.
    public void sendAuthCode(String email, MailPurpose purpose) {

        // 1단계 : 쿨타임 확인
        String cooldownKey = buildCooldownKey(email, purpose); // 이 이메일+목적 전용 쿨타임 키 문자열 조립
        Long remainingSeconds = redisTemplate.getExpire(cooldownKey);
        // getExpire(키) : 그 키의 남은 만료시간(초)을 돌려줌. 키가 없으면 -2, 만료시간 설정이 없으면 -1

        if (remainingSeconds != null && remainingSeconds > 0) { // 참이면 = 아직 쿨타임 살아있음.
            throw new MailCooldownException(remainingSeconds); // 실제 발송 시도(코드 생성, 메일 전송)를 하나도 안 하고 여기서 바로 중단
        }

        // 2단계 : 쿨타임 키 새로 세팅(60초)
        redisTemplate.opsForValue().set(cooldownKey, "1", COOLDOWN_TTL);
        // 메일 발송 실패해도(SMTP 문제 등) 60초 동안은 재시도를 못 하게 막고 문제가 있는 상태에서 서버에 요청이 몰리는 걸 방지하기 위해서.

        // 3단계 : 오입력 횟수 초기화
        String failCountKey = buildFailCountKey(email, purpose); // 오입력 횟수 세는 전용 키
        redisTemplate.delete(failCountKey); // 새 인증번호 발급하는 시점 -> 이전 실패 횟수 의미 없으니 삭제.

        // 4단계 : 6자리 인증번호 생성
        // %0Nd : 숫자를 N자리로 맞추고, 모자란 앞자리는 0으로 채움.(예 : 42 -> "000042")
        String authCode = String.format("%0" + AUTH_CODE_LENGTH + "d", secureRandom.nextInt(AUTH_CODE_BOUND));

        // 5단계 : Redis 인증번호 저장
        String authKey = buildAuthKey(email, purpose); // 인증번호 자체를 저장할 키
        redisTemplate.opsForValue().set(authKey, authCode, AUTH_CODE_TTL); // 3분 TTL로 저장

        // 6단계 : 실제 메일 발송 시도
        try {
            sendMail(email, "[secondpro] 이메일 인증번호", buildMailBody(authCode));
        } catch (RuntimeException e) { // sendMail 내부에서 던진 RuntimeException을 여기서 잡음
            redisTemplate.delete(authKey); // 발송 안 된 인증번호는 3분 기다리지 않고 즉시 삭제
            throw e; // 잡은 예외 그대로 다시 던짐(위쪽 Controller가 잡아서 사용자에게 안내 문구 보여줌)
        }
    }

    // 사용자가 입력한 인증번호 맞는지 확인 -> 맞으면 인증번호 키 즉시 삭제(재사용 방지), 인증 완료 키를 10분짜리로 새로 생성
    public boolean verifyAuthCode(String email, MailPurpose purpose, String inputCode) {

        String authKey = buildAuthKey(email, purpose);
        String savedCode = redisTemplate.opsForValue().get(authKey);
        // get(키) -> 저장된 값 꺼내옴. 키가 없거나(3분 후 만료) 발송 이력 없으면 null.

        if (savedCode == null) { // 키 없음 = 발송한 적 없거나, 3분 지나서 만료거나, 5회 오입력으로 무효화된 경우
            return false;
        }

        if (!savedCode.equals(inputCode)) { // 저장된 값과 사용자가 입력한 값 다르다
            registerFailAttempt(email, purpose, authKey);
            return false;
        }

        redisTemplate.delete(authKey); // 재사용 방지. 인증 성공한 코드는 여기서 즉시 삭제.(3분 만료 기다리지 않고 바로 무효화)
        redisTemplate.delete(buildFailCountKey(email, purpose)); // 성공했으니 오입력 카운트 함께 정리(신규)

        String verifiedKey = buildVerifiedKey(email, purpose);
        redisTemplate.opsForValue().set(verifiedKey, "true", VERIFIED_TTL); // "인증 완료" 상태를 10분짜리로 새로 기록

        return true;
    }

    // 오입력 1회 기록, 5회 넘으면 인증번호 자체 무효화 메서드
    private void registerFailAttempt(String email, MailPurpose purpose, String authKey) {

        String failCountKey = buildFailCountKey(email, purpose);
        Long failCount = redisTemplate.opsForValue().increment(failCountKey);
        // increment(키) : 그 키의 숫자값을 1 증가시키고, 증가된 후의 값을 돌려줌.
        // 키 아예 없으면 0에서 시작해서 1이 됨

        if (failCount != null && failCount == 1L) { // 방금 이 시도가 "첫 번째" 오입력이었다면
            redisTemplate.expire(failCountKey, AUTH_CODE_TTL);
            // increment는 키를 처음 만들 때 만료시간을 자동으로 안 붙여줌. -> 직접 설정
            // AUTH_CODE_TTL(3분)과 맞추는 이유 : 인증번호가 사라지는 시점에 오입력 카운트도 같이 사라지는 게 자연스러움.
        }

        if (failCount != null && failCount >= MAX_FAIL_COUNT) { // 누적 5회 이상 틀리면
            redisTemplate.delete(authKey); // 정답 인증번호 자체를 지움 -> 이후 검증은 계속 false(만료와 동일하게 취급)
        }
    }

    // 이 이메일이 이 목적으로 방금 인증을 완료한 상태인가?를 확인.
    // 비밀번호 재설정 화면 접근 허용, 아아디 찾기 결과 화면 접근 허용 여부를 Controller가 판단할 때 사용.
    public boolean isVerified(String email, MailPurpose purpose) {
        String verifiedKey = buildVerifiedKey(email, purpose);
        Boolean exists = redisTemplate.hasKey(verifiedKey);
        // hashKey(키) -> 있으면 true, 없으면 false 반환.
        return Boolean.TRUE.equals(exists); // exists가 nul이어도 예외 없이 false로 취금.
    }

    // 비밀번호 변경까지 완전히 끝난 뒤, 인증 완료 상태를 지움.
    public void clearVerified(String email, MailPurpose purpose) {
        redisTemplate.delete(buildVerifiedKey(email, purpose));
    }

    private String buildAuthKey(String email, MailPurpose purpose) {
        return "mail:auth:" + purpose.getKeyName() + ":" + email;
        // 예 : email="a@naver.com", purpose=FIND_ID
        // -> "mail:auth:" + "findId" + ":" + "a@naver.com" -> "mail:auth:findId:a@naver.com"
    }

    // Redis 키 문자열 조림하는 private 메서드들
    private String buildVerifiedKey(String email, MailPurpose purpose) {
        return "mail:verified:" + purpose.getKeyName() + ":" + email;
        // 예 : email="a@naver.com", purpose=RESET_PW -> "mail:verified:resetPw:a@naver.com"
    }

    private String buildCooldownKey(String email, MailPurpose purpose) {
        return "mail:cooldown:" + purpose.getKeyName() + ":" + email;
        // 예 : "mail:cooldown:findId:a@naver.com"
    }

    private String buildFailCountKey(String email, MailPurpose purpose) {
        return "mail:failCount:" + purpose.getKeyName() + ":" + email;
    }

    private String buildMailBody(String authCode) {
        return "요청하신 인증번호는 [" + authCode + "] 입니다.\n3분 이내에 입력해 주세요.";
        // 문자열 + 는 "이어붙이기". \n은 줄바꿈 문자.
    }

    private void sendMail(String to, String subject, String body) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            // createMimeMesage() -> 빈 편지지 한 장을 새로 만드는 것(아직 내용 X)

            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            // 1번 인자 : 방금 만든 빈 편지지 / 2번 인자 : false -> 첨부파일/HTML 없는 "단순 텍스트 메일"
            // 3번 인자 : "UTF-8" -> secret.yaml의 default-encoding과 같은 값을 여기서도 직접 지정. 한글 제목, 본문 안 깨지게.

            helper.setTo(to); // 받는 사람 = 사용자 입력 이메일(매개변수 to)
            helper.setFrom(fromAddress); // 보내는 사람 = @Value로 주입 받은 네이버 계정 주소
            helper.setSubject(subject); // 편지 제목
            helper.setText(body); // 편지 본문

            mailSender.send(mimeMessage);
            // secret.yaml에 적어 둔 host:port(465)로 실제 접속해서 편지 전송. 이 메서드 안에서 유일하게 "네트워크 통신" 일어나는 지점.
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 발송에 실패했습니다. 이메일 주소를 확인해 주세요.", e);
            // sendAuthCode()의 catch가 이걸 잡는다.
        }
    }
}
