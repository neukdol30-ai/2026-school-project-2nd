package com.siyan1234.itproject2nd.map.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 카카오맵 API 설정값입니다.
 *
 * restApiKey:
 * - Spring 서버가 카카오 REST API를 호출할 때 사용합니다.
 * - 브라우저에 노출하면 안 됩니다.
 *
 * javascriptKey:
 * - Kakao Map JavaScript SDK를 브라우저에서 로드할 때 사용합니다.
 * - 카카오 개발자센터에서 사이트 도메인을 반드시 등록해야 합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kakao.map")
public class KakaoMapApiProperties {

    /** 카카오 개발자센터에서 발급받은 REST API 키 */
    private String restApiKey;
    private String javascriptKey;
    /** 카카오 서버 연결 대기 시간 */
    private int connectTimeoutSeconds = 5;
    /** 카카오 서버 응답 대기 시간 */
    private int readTimeoutSeconds = 10;

    public boolean hasRestApiKey() {
        return StringUtils.hasText(restApiKey);
    }

    public boolean hasJavascriptKey() {
        return StringUtils.hasText(javascriptKey);
    }

    public String authorizationHeaderValue() {
        return "KakaoAK " + restApiKey;
    }
}
