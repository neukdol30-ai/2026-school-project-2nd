package com.siyan1234.itproject2nd.config;

import com.siyan1234.itproject2nd.config.handler.*;
import com.siyan1234.itproject2nd.config.security.AdminSessionGuardFilter;
import com.siyan1234.itproject2nd.config.security.SecurityAuthority;
import com.siyan1234.itproject2nd.config.security.SecurityPaths;
import com.siyan1234.itproject2nd.member.service.OAuth2DetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Spring Security 설정입니다.
 * <p>
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
    private final AdminSessionGuardFilter adminSessionGuardFilter;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // 소셜 로그인 성공 전용 Handler
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler; // 소셜 로그인 실패 전용 Handler
    private final OAuth2DetailsService oAuth2DetailsService;

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
                // 2-2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/member/login")
                        .successHandler(oAuth2LoginSuccessHandler) // 소셜 로그인 성공 시 최근 로그인 시각 갱신 후 메인으로 이동
                        .failureHandler(oAuth2LoginFailureHandler) // 실패 사유를 세션에 담고 로그인 화면으로 보냄
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2DetailsService))
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
                )
                // 로그인 이후 관리자 권한이 USER로 변경되어도 /admin 기능을 계속 쓰지 못하도록 DB 기준 권한을 재검증
                .addFilterAfter(adminSessionGuardFilter, AuthorizationFilter.class);

        return http.build();
    }
}
