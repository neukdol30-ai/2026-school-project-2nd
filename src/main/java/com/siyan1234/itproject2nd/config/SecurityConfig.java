package com.siyan1234.itproject2nd.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/temp/**",
                                "/chat/**",
                                "/ws/**",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}

///멤버 연결후 코드변경 ///
//@Configuration
//public class SecurityConfig {
//
//    @Bean
//    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
//
//        http
//                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .requestMatchers(
//                                "/",
//                                "/member/login",
//                                "/member/signup",
//                                "/oauth2/**",
//                                "/login/oauth2/**",
//                                "/css/**",
//                                "/js/**",
//                                "/images/**"
//                        ).permitAll()
//
//                        .requestMatchers("/chat/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/chat/**").authenticated()
//                        .requestMatchers("/ws/**").authenticated()
//
//                        .anyRequest().authenticated()
//                )
//                .formLogin(form -> form
//                        .loginPage("/member/login")
//                        .permitAll()
//                )
//                .logout(logout -> logout
//                        .logoutUrl("/member/logout")
//                        .logoutSuccessUrl("/")
//                );
//
//        return http.build();
//    }
//}