package com.siyan1234.itproject2nd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
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
                                "/index.html",
                                "/member/login", // 일반 사용자 로그인 화면
                                "/member/signup", // 회원가입 화면
                                "/member/exists", // 아이디 중복 확인
                                "/admin/login", // 관리자 로그인 화면
                                "/kakao/authorize", // 카카오 동의
                                "/kakao/callback", // 카카오 인가 코드 토큰발급
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/api/**",
                                "/member/exists-nickname", // 닉네임 중복 확인(회원가입 중 = 로그인 전에도 호출) 없으면 403
                                "/error" // 필수, 예외 발생 시 Spring Boot가 /error로 내부 포워딩. Security 6은 그 포워딩도 인가 재검사. 없으면 비로그인 상태 예외 -> 에러 화면 대신 로그인으로 302 (에러 은폐)
                        ).permitAll()
                        .requestMatchers("/kakao/test-message").hasRole("ADMIN")
                        .requestMatchers("/chat/admin/**").hasRole("ADMIN")
                        .requestMatchers("/chat/**").authenticated()
                        .requestMatchers("/ws/**").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN") // /admin으로 시작하는 주소는 ROLE_ADMIN 권한자만 접근
                        .anyRequest().authenticated() // 위에서 안 걸린 나머지 주소는 로그인한 사람만 접근
                )
                .csrf(csrf -> csrf.disable())
                // 2 로그인 폼 설정
                .formLogin(form -> form
                        .loginPage("/member/login") // 로그인 화면 주소(GET)
                        .loginProcessingUrl("/member/login") // 로그인 처리 주소(POST), Security 자동 처리
                        .usernameParameter("memberId") // 폼의 아이디 입력칸 name과 일치
                        .passwordParameter("password") // 폼의 비번 입력칸 name와 일치
                        .successHandler((request, response, authentication) -> {
                            String contextPath = request.getContextPath();
                            String loginType = request.getParameter("loginType");

                            boolean isAdmin = authentication.getAuthorities()
                                    .stream()
                                    .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

                            /*
                                관리자 로그인 화면에서 로그인한 경우입니다.
                                ADMIN 권한이면 /admin으로 이동하고,
                                일반 USER 계정이면 관리자 세션을 만들지 않고 다시 /admin/login으로 보냅니다.
                             */
                            if ("admin".equals(loginType)) {
                                if (isAdmin) {
                                    response.sendRedirect(contextPath + "/admin");
                                    return;
                                }

                                SecurityContextHolder.clearContext();
                                if (request.getSession(false) != null) {
                                    request.getSession(false).invalidate();
                                }
                                response.sendRedirect(contextPath + "/admin/login?error=role");
                                return;
                            }

                            // 일반 사용자 로그인은 기존 흐름처럼 서비스 메인으로 이동합니다.
                            response.sendRedirect(contextPath + "/");
                        })
                        .failureHandler((request, response, exception) -> {
                            String contextPath = request.getContextPath();
                            String loginType = request.getParameter("loginType");

                            if ("admin".equals(loginType)) {
                                response.sendRedirect(contextPath + "/admin/login?error=true");
                                return;
                            }

                            response.sendRedirect(contextPath + "/member/login?error=true");
                        })
                        .permitAll()
                )
                // 3 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/member/logout") // 이 주소로 POST 요청 시 로그아웃
                        .logoutSuccessUrl("/") // 로그아웃 후 메인으로
                        .invalidateHttpSession(true) // 로그인 상태 세션 완전 삭제
                )
                // 4 관리자 주소 접근 시 관리자 로그인 화면으로 이동시키기 위한 예외 처리
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            String contextPath = request.getContextPath();
                            String requestUri = request.getRequestURI();

                            if (requestUri.startsWith(contextPath + "/admin")
                                    || requestUri.startsWith(contextPath + "/chat/admin")) {
                                response.sendRedirect(contextPath + "/admin/login");
                                return;
                            }

                            response.sendRedirect(contextPath + "/member/login");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            String contextPath = request.getContextPath();
                            String requestUri = request.getRequestURI();

                            if (requestUri.startsWith(contextPath + "/admin")
                                    || requestUri.startsWith(contextPath + "/chat/admin")) {
                                response.sendRedirect(contextPath + "/admin/login?error=role");
                                return;
                            }

                            response.sendRedirect(contextPath + "/member/login?error=denied");
                        })
                );

        return http.build();
    }
}
