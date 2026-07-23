package com.siyan1234.itproject2nd.map.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 카카오맵 REST API 호출에 필요한 설정값입니다.
 *
 * 실제 REST API 키는 application.yaml에 직접 쓰지 않고 환경변수 KAKAO_REST_API_KEY로 주입합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.map")
public class KakaoMapApiProperties {

    /** 카카오 개발자센터에서 발급받은 REST API 키 */
    private String restApiKey;

    /** 카카오 서버 연결 대기 시간 */
    private int connectTimeoutSeconds = 5;

    /** 카카오 서버 응답 대기 시간 */
    private int readTimeoutSeconds = 10;

    public boolean hasRestApiKey() {
        return StringUtils.hasText(restApiKey);
    }

    public String authorizationHeaderValue() {
        return "KakaoAK " + restApiKey.trim();
    }
}
