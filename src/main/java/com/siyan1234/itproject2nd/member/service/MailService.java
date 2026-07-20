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
    @Value("${spring.mail.username}")
    private String fromAddress;

    private static final int AUTH_CODE_LENGTH = 6; // 인증번호 자릿수
    private static final int AUTH_CODE_BOUND = (int) Math.pow(10, AUTH_CODE_LENGTH); // 6자리 -> 1,000,000
    private static final Duration AUTH_CODE_TTL = Duration.ofMinutes(3); // 인증번호 유효시간
    private static final Duration VERIFIED_TTL = Duration.ofMinutes(10); // "인증 완료" 상태 유지시간

    private final SecureRandom secureRandom = new SecureRandom(); // 난수 생성기, 남이 맞히면 안 되는 값엔 반드시 써야 함.

    public enum MailPurpose {
        FIND_ID("findId"), // 아이디 찾기용 Redis 키 이름표
        RESET_PW("resetPw"); // 비밀번호 찾기(재설정)용

        private final String keyName;

        MailPurpose(String keyName) {
            this.keyName = keyName;
        }

        public String getKeyName() {
            return keyName;
        }
    }

    // 인증번호 생성 -> 이메일로 보내고, Redis에 3분짜리로 저장.
    // 메일 발송 실패하면 방금 저장한 인증번호 즉시 지움.
    public void sendAuthCode(String email, MailPurpose purpose) {
        // 1단계 : 6자리 인증번호 생성 / ^06d -> 숫자 6자리 맞추고 모자란 앞자리는 0으로 채움.
        String authCode = String.format("%0" + AUTH_CODE_LENGTH + "d", secureRandom.nextInt(AUTH_CODE_BOUND));

        // 2단계 : Redis 저장
        String authKey = buildAuthKey(email, purpose);
        redisTemplate.opsForValue().set(authKey, authCode, AUTH_CODE_TTL);

        // 3단계 : 실제 메일 발송 시도. 실패하면 2단계에서 저장한 인증번호 되돌림
        try {
            sendMail(email, "[secondpro] 이메일 인증번호", buildMailBody(authCode));
        } catch (RuntimeException e) {
            redisTemplate.delete(authKey);
            // 이 조건이 참이면 실행 : 발송 안 된 인증번호는 3분 기다리지 않고 바로 삭제.
            throw e;
        }
    }

    // 사용자가 입력한 인증번호 맞는지 확인 -> 맞으면 인증번호 키 즉시 삭제(재사용 방지), 인증 완료 키를 10분짜리로 새로 생성
    public boolean verifyAuthCode(String email, MailPurpose purpose, String inputCode) {

        String authKey = buildAuthKey(email, purpose);
        String savedCode = redisTemplate.opsForValue().get(authKey);
        // get(키) -> 저장된 값 꺼내옴. 키가 없거나(3분 후 만료) 발송 이력 없으면 null.

        if (savedCode == null) {
            return false;
        }

        if (!savedCode.equals(inputCode)) {
            return false;
        }

        redisTemplate.delete(authKey);
        // 재사용 방지. 인증 성공한 코드는 여기서 즉시 삭제.(3분 만료 기다리지 않고 바로 무효화)

        String verifiedKey = buildVerifiedKey(email, purpose);
        redisTemplate.opsForValue().set(verifiedKey, "true", VERIFIED_TTL);

        return true;
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

    private String buildVerifiedKey(String email, MailPurpose purpose) {
        return "mail:verified:" + purpose.getKeyName() + ":" + email;
        // 예 : email="a@naver.com", purpose=RESET_PW -> "mail:verified:resetPw:a@naver.com"
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
