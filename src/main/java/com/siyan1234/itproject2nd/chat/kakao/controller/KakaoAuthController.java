package com.siyan1234.itproject2nd.chat.kakao.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 카카오 메시지 전송용 토큰 발급 Controller
 *
 * 역할:
 * - 관리자 카카오 계정으로 talk_message 권한 동의
 * - 인가 코드 발급
 * - access_token, refresh_token 발급
 *
 * 사용 순서:
 * 1. 브라우저에서 /kakao/authorize 접속
 * 2. 카카오 로그인 및 talk_message 권한 동의
 * 3. /kakao/callback 응답에서 refresh_token 확인
 * 4. refresh_token을 환경변수 KAKAO_ADMIN_REFRESH_TOKEN에 등록
 *
 * 주의:
 * - 이 Controller는 개발/테스트용입니다.
 * - refresh_token 발급 후 application.yml에 저장하면 됩니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/kakao")
public class KakaoAuthController {

    @Value("${kakao.notify.rest-api-key}")
    private String restApiKey;

    @Value("${kakao.notify.redirect-uri:http://localhost:8080/kakao/callback}")
    private String redirectUri;

    @Value("${kakao.notify.client-secret:}")
    private String clientSecret;

    private final RestClient restClient = RestClient.create();

    /**
     * 카카오 인가 코드 요청
     *
     * 카카오 로그인 화면으로 이동하고 talk_message 권한 동의를 요청한다.
     */
    @GetMapping("/authorize")
    public String authorize() {
        if (restApiKey == null || restApiKey.isEmpty()) {
            throw new IllegalStateException("KAKAO_REST_API_KEY 환경변수가 설정되지 않았습니다.");
        }
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        String kakaoAuthorizeUrl =
                "https://kauth.kakao.com/oauth/authorize"
                        + "?client_id=" + restApiKey
                        + "&redirect_uri=" + encodedRedirectUri
                        + "&response_type=code"
                        + "&scope=talk_message";

        return "redirect:" + kakaoAuthorizeUrl;
    }

    /**
     * 2단계:
     * 카카오 로그인 성공 후 카카오가 이 주소로 code를 보내준다.
     *
     * 예:
     * /kakao/callback?code=xxxxx
     *
     * 이 code를 access_token, refresh_token으로 교환한다.
     */
    @ResponseBody
    @GetMapping("/callback")
    public Map<String, Object> callback(@RequestParam String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "authorization_code");
        formData.add("client_id", restApiKey);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);

        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }

        Map<String, Object> response = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        log.info("카카오 토큰 응답 = {}", response);

        return response;
    }
}