package com.siyan1234.itproject2nd.cookie.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 방문 기록 저장용 DTO
 *
 * 분석 쿠키에 동의한 경우에만 VisitLogInterceptor가 이 DTO를 만들어 DB에 저장합니다.
 */
@Getter
@Setter
public class VisitLogDto {

    private Long no;
    private Integer memberNo;
    private String sessionId;
    private String requestUri;
    private String queryString;
    private String requestMethod;
    private String userAgent;
    private String ipAddress;
    private String referer;
    private LocalDateTime createdDate;
}
