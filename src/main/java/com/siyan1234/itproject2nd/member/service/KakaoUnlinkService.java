package com.siyan1234.itproject2nd.member.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.regex.Pattern;

@Service
@Slf4j
public class KakaoUnlinkService {

    // 카카오 연결 해제 API 주소 한 곳에서 관리
    private static final String KAKAO_UNLINK_URL =
            "https://kapi.kakao.com/v1/user/unlink";

    private static final Pattern KAKAO_ALREADY_UNLINKED_PATTERN =
            Pattern.compile("\"code\"\\s*:\\s*-101\\b");

    // 생성자에서 타임아웃 적용한 RestClient를 만들어 저장
    private final RestClient restClient;

    @Value("${kakao.admin-key}")
    private String adminKey;

    // 연결 제한 3초 : 카카오 서버와 연결 자체가 3초 안에 안 되면 요청 중단.
    // 응답 제한 5초 : 연결된 후에도 5초 안에 응답이 안 오면 요청 중단.
    public KakaoUnlinkService() {

        // 외부 HTTP 요청에 사용할 설정 객체 생성
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(3));

        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    // 카카오 회원 고유번호 이용해서 실제 카카오 앱 연결 해제
    public boolean unlinkByAdminKey(String providerId) {

        // 어드민 키 비었으면 카카오 API 호출 불가
        if (adminKey == null || adminKey.isBlank()) {
            log.error("카카오 연결 해제 실패 : kakao.admin-key 설정값이 없습니다.");
            return false;
        }

        // 카카오 회원 고유 번호 없으면 누구의 연결 끊을지 지정할 수 없음.
        if (providerId == null || providerId.isBlank()) {
            log.error("카카오 연결 해제 실패 : providerId가 없습니다.");
            return false;
        }
        // application/x-www-form-urlencoded 형식으로 보낼 요청 본문 생성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        // target_id가 카카오 회원 번호라는 사실을 카카오 서버에 알림
        formData.add("target_id_type", "user_id");

        // social_account.provider_id에서 조회한 실제 카카오 회원번호
        formData.add("target_id", providerId);

        try {
            restClient.post() // Spring Boot 서버에서 카카오 연결 해제 서버로 POST 요청.
                    .uri(KAKAO_UNLINK_URL) // 연결 해제 API 주소 지정
                    .header(
                            HttpHeaders.AUTHORIZATION,
                            "KakaoAK " + adminKey
                    ) // 카카오 어드민 키 인증 방식 Authorization 헤더에 넣음
                    .contentType(
                            MediaType.APPLICATION_FORM_URLENCODED
                    ) // 요청 본문이 HTML form과 같은 key=value 형식임을 알림
                    .body(formData) // 위에서 만든 target_id_type과 target_id를 요청 본문에 넣음
                    .retrieve() // 실제 요청 보내고 HTTP 응답 받음
                    .toBodilessEntity(); // 응답 본문 사용하지 않고 성공 여부만 확인

            // 예외 없이 정상 응답을 받았다면 연결 해제 성공
            return true;

            // 부모 예외를 먼저 잡으면 이 블록까지 도달 X
        } catch (RestClientResponseException e) {

            String responseBody = e.getResponseBodyAsString();

            // 핵심 예외 처리
            if (e.getStatusCode().value() == 400
                    && isAlreadyUnlinked(responseBody)) {

                log.info("카카오 계정은 이미 연결 해제된 상태입니다.");

                return true;
            }

            log.error("카카오 연결 해제 응답 오류. status={}, body={}",
                    e.getStatusCode(),
                    responseBody
            );

            return false;

        } catch (RestClientException e) {

            log.error("카카오 연결 해제 API 통신에 실패했습니다.",
                    e
            );

            return false;
        }
    }

    // 카카오 오류 응답에 code: -101이 포함됐는지 확인
    private boolean isAlreadyUnlinked(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            return false;
        }

        return KAKAO_ALREADY_UNLINKED_PATTERN
                .matcher(responseBody)
                .find();
    }
}