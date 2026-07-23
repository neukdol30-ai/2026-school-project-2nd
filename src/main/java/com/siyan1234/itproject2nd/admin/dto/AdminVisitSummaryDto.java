package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 관리자 콘솔 방문 기록 사용자별 요약 DTO입니다. */
@Getter
@Setter
public class AdminVisitSummaryDto {

    private Integer memberNo;
    private String memberId;
    private String name;
    private String nickname;
    private String role;
    private String ipAddress;
    private Long visitCount;
    private LocalDateTime firstVisitDate;
    private LocalDateTime lastVisitDate;
    private String lastRequestUri;
    private String lastUserAgent;

    public String getDisplayUser() {
        if (memberId != null && !memberId.isBlank()) {
            return memberId;
        }
        return "비로그인 방문자";
    }

    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (name != null && !name.isBlank()) {
            return name;
        }
        return "-";
    }
}