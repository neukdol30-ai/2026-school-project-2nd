package com.siyan1234.itproject2nd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration // 이 클래스가 "설정 담당"이라고 스프링에 전달 -> 안의 @Bean을 등록.
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt: 비밀번호 해싱
        return new BCryptPasswordEncoder();
    }
}
