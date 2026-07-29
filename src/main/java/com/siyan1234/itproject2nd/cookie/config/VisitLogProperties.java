package com.siyan1234.itproject2nd.cookie.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 방문 기록 기능에서 사용하는 공통 설정입니다.
 *
 * <p>프록시 전달 헤더 신뢰 여부와 원본 방문 로그 보관기간을
 * 하나의 설정 빈으로 관리합니다.</p>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.visit-log")
public class VisitLogProperties {

    /**
     * X-Forwarded-For, X-Real-IP 헤더를 신뢰할지 여부입니다.
     * 신뢰 가능한 프록시가 헤더를 덮어쓰는 운영 환경에서만 활성화합니다.
     */
    private boolean trustForwardedHeaders = false;

    /** IP, User-Agent 등이 포함된 원본 방문 로그 보관 일수입니다. */
    private int retentionDays = 30;

    /** 잘못된 설정으로 0일 이하가 들어와도 최소 1일은 보관합니다. */
    public int getSafeRetentionDays() {
        return Math.max(retentionDays, 1);
    }
}
