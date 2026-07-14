package com.siyan1234.itproject2nd.config;

import com.siyan1234.itproject2nd.config.handler.CustomAccessDeniedHandler;
import com.siyan1234.itproject2nd.config.handler.CustomAuthenticationEntryPoint;
import com.siyan1234.itproject2nd.config.handler.CustomLoginFailureHandler;
import com.siyan1234.itproject2nd.config.handler.CustomLoginSuccessHandler;
import com.siyan1234.itproject2nd.config.security.SecurityAuthority;
import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정입니다.
 *
 * URL 그룹은 SecurityPaths에서 관리하고,
 * 로그인 성공/실패/권한 오류 이동 처리는 handler 패키지에서 담당합니다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomLoginSuccessHandler customLoginSuccessHandler;
    private final CustomLoginFailureHandler customLoginFailureHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1 인가 규칙: 어떤 주소를 누구에게 허용?
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityPaths.PUBLIC_MATCHERS).permitAll()
                        .requestMatchers("/kakao/test-message").hasRole(SecurityAuthority.ADMIN)
                        .requestMatchers("/chat/admin/**").hasRole(SecurityAuthority.ADMIN)
                        .requestMatchers("/admin/**").hasRole(SecurityAuthority.ADMIN)
                        .requestMatchers("/chat/**").authenticated()
                        .requestMatchers("/ws/**").authenticated()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                // 2 로그인 폼 설정
                .formLogin(form -> form
                        .loginPage(SecurityPaths.MEMBER_LOGIN) //로그인 화면 주소(GET)
                        .loginProcessingUrl(SecurityPaths.MEMBER_LOGIN) //로그인 처리 주소(POST), Security 자동 처리
                        .usernameParameter("memberId") // 폼의 아이디 입력칸 name과 일지
                        .passwordParameter("password") // 폼의 비번 입력칸 name와 일치
                        .successHandler(customLoginSuccessHandler)
                        .failureHandler(customLoginFailureHandler)
                        .permitAll()
                )
                // 3 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/member/logout") // 이 주소로 POST 요청 시 로그아웃
                        .logoutSuccessUrl(SecurityPaths.HOME) // 로그아웃 후 메인으로
                        .invalidateHttpSession(true) // 로그인 상태 세션 완전 삭제
                )
                // 4 관리자 주소 접근 시 관리자 로그인 화면으로 이동시키기 위한 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }
}
