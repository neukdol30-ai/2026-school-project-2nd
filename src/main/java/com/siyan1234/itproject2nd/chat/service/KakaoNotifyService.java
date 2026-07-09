package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 카카오톡 나에게 보내기 알림 Service
 *
 * 역할:
 * - 새 1:1 상담방이 생성되었을 때 관리자 카카오톡 나와의 채팅방으로 알림 전송
 *
 * 주의:
 * - 일반 카카오톡 API로 임의의 휴대폰 번호에 메시지를 보내는 것은 불가능하다.
 * - 이 방식은 관리자 본인의 카카오 계정 refresh_token을 이용해
 *   관리자 본인 "나와의 채팅방"으로 메시지를 보내는 방식이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoNotifyService {

    @Value("${kakao.notify.enabled:false}")
    private boolean enabled;

    @Value("${kakao.notify.rest-api-key:}")
    private String restApiKey;

    @Value("${kakao.notify.client-secret:}")
    private String clientSecret;

    @Value("${kakao.notify.admin-refresh-token:}")
    private String adminRefreshToken;

    @Value("${kakao.notify.admin-room-base-url:http://localhost:8080/chat/admin}")
    private String adminRoomBaseUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * 새 상담방 생성 알림 전송
     */
    public void sendNewChatRoomAlert(ChatRoomDto chatRoom) {
        if (!enabled) {
            return;
        }

        if (chatRoom == null || chatRoom.getRoomNo() == null) {
            return;
        }

        try {
            String accessToken = refreshAccessToken();
            sendMemo(accessToken, chatRoom);

            log.info("카카오톡 상담 알림 전송 완료 roomNo={}", chatRoom.getRoomNo());
        } catch (Exception e) {
            /*
                카카오톡 알림 실패 때문에 채팅 기능 자체가 실패하면 안 된다.
                그래서 예외를 밖으로 던지지 않고 로그만 남긴다.
            */
            log.warn("카카오톡 상담 알림 전송 실패 roomNo={}", chatRoom.getRoomNo(), e);
        }
    }

    /**
     * refresh_token으로 access_token 재발급
     *
     * 카카오 API 호출에는 access_token이 필요하지만 만료 시간이 짧기 때문에
     * 서버에서는 refresh_token으로 access_token을 재발급해서 사용한다.
     */
    private String refreshAccessToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        formData.add("grant_type", "refresh_token");
        formData.add("client_id", restApiKey);
        formData.add("refresh_token", adminRefreshToken);

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

        if (response == null || response.get("access_token") == null) {
            throw new IllegalStateException("카카오 access_token 재발급 실패");
        }

        return response.get("access_token").toString();
    }

    /**
     * 카카오톡 나에게 보내기 API 호출
     */
    private void sendMemo(String accessToken, ChatRoomDto chatRoom) {
        String adminRoomUrl = adminRoomBaseUrl + "/" + chatRoom.getRoomNo();

        String messageText = """
                [SecondPro 상담 알림]
                새 1:1 문의가 도착했습니다.

                상담방 번호: #%s
                문의 유형: %s
                사용자 번호: %s
                """.formatted(
                chatRoom.getRoomNo(),
                chatRoom.getCategoryName(),
                chatRoom.getUserNo()
        );

        String templateObject = """
                {
                  "object_type": "text",
                  "text": "%s",
                  "link": {
                    "web_url": "%s",
                    "mobile_web_url": "%s"
                  },
                  "button_title": "상담 확인"
                }
                """.formatted(
                escapeJson(messageText),
                escapeJson(adminRoomUrl),
                escapeJson(adminRoomUrl)
        );

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("template_object", templateObject);

        String response = restClient.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Bearer " + accessToken)
                .body(formData)
                .retrieve()
                .body(String.class);

        log.info("카카오톡 나에게 보내기 응답 = {}", response);
    }

    /**
     * JSON 문자열 안에 들어갈 값 이스케이프 처리
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}