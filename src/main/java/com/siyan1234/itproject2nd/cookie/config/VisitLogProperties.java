package com.siyan1234.itproject2nd.cookie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 방문 기록에서 요청자의 IP를 판별할 때 사용하는 설정입니다.
 *
 * X-Forwarded-For와 X-Real-IP는 클라이언트가 임의로 전송할 수도 있으므로,
 * 신뢰 가능한 리버스 프록시가 해당 헤더를 덮어쓰는 운영 환경에서만 활성화합니다.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.visit-log")
public class VisitLogProperties {

    private boolean trustForwardedHeaders = false;
}
