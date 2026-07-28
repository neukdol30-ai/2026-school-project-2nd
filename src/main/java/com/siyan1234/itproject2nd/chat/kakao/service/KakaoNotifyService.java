package com.siyan1234.itproject2nd.chat.kakao.service;

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
 * - 새 1:1 상담방이 생성되었을 때
 * - 관리자 카카오 계정의 "나와의 채팅방"으로 알림 전송
 *
 * 구조:
 * refresh_token으로 access_token 재발급
 * → 카카오톡 나에게 보내기 API 호출
 *
 * 주의:
 * - 카카오톡 API로 임의의 휴대폰 번호에 메시지를 보내는 것은 불가능하다.
 * - 이 방식은 관리자 본인의 카카오 계정으로 "나에게 보내기"를 하는 방식이다.
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
            log.info("카카오 알림 비활성화 상태입니다.");
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
        if (restApiKey == null || restApiKey.isEmpty()) {
            throw new IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
        }
        if (adminRefreshToken == null || adminRefreshToken.isEmpty()) {
            throw new IllegalStateException("KAKAO_ADMIN_REFRESH_TOKEN이 설정되지 않았습니다.");
        }

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
        String adminRoomUrl = createAdminRoomUrl(chatRoom.getRoomNo());

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

        /*
         * 카카오 응답 본문 전체를 로그로 남기지 않습니다.
         * 전송 성공 여부는 호출자 sendNewChatRoomAlert()의 완료 로그로 확인합니다.
         */
        restClient.post()
                .uri("https://kapi.kakao.com/v2/api/talk/memo/default/send")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .header("Authorization", "Bearer " + accessToken)
                .body(formData)
                .retrieve()
                .body(String.class);
    }

    /** 관리자 상담방 상세 주소를 슬래시 중복 없이 생성합니다. */
    private String createAdminRoomUrl(Integer roomNo) {
        if (adminRoomBaseUrl.endsWith("/")) {
            return adminRoomBaseUrl + roomNo;
        }
        return adminRoomBaseUrl + "/" + roomNo;
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