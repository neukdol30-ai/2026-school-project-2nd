package com.siyan1234.itproject2nd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // 1 인가 규칙: 어떤 주소를 누구에게 허용?
                .authorizeHttpRequests(auth -> auth
                        // 로그인 없이 누구나 접근 가능한 주소들
                        .requestMatchers(
                                "/", // 메인
                                "/member/login", // 로그인 화면
                                "/member/signup", // 회원가입 화면
                                "/css/**", "/js/**", "/images/**" // 정적 파일
                        ).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN") // /admin으로 시작하는 주소는 ROLE_ADMIN 권한자만 접근
                        .anyRequest().authenticated() // 위에서 안 걸린 나머지 주소는 로그인한 사람만 접근
                )
                // 2 로그인 폼 설정
                .formLogin(form -> form
                        .loginPage("/member/login") // 로그인 화면 주소(GET)
                        .loginProcessingUrl("/member/login") // 로그인 처리 주소(POST), Security 자동 처리
                        .usernameParameter("memberId") // 폼의 아이디 입력칸 name과 일치
                        .passwordParameter("password") // 폼의 비번 입력칸 name와 일치
                        .defaultSuccessUrl("/", true) // 로그인 성공 시 메인으로 이동
                        .failureUrl("/member/login?error=true") // 실패 시 다시 로그인 화면으로.
                        .permitAll()
                )
                // 3 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/member/logout") // 이 주소로 POST 요청 시 로그아웃
                        .logoutSuccessUrl("/") // 로그아웃 후 메인으로
                );
        // csrf는 켠 채로 둔다(기본값).
        return http.build();
    }
}