package com.siyan1234.itproject2nd.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 외부 서비스 설정 상태 표시용 DTO입니다. 실제 키 값은 화면에 노출하지 않습니다. */
@Getter
@AllArgsConstructor
public class ServiceStatusDto {

    private String serviceName;
    private String description;
    private String status;
    private boolean available;
}
