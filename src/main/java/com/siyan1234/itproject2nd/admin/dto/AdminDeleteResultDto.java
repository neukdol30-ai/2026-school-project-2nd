package com.siyan1234.itproject2nd.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 관리자 콘솔 선택 삭제 결과를 담는 DTO입니다. */
@Getter
@AllArgsConstructor
public class AdminDeleteResultDto {

    private final int requestedCount;
    private final int deletedCount;
    private final int skippedCount;

    public boolean hasDeletedItem() {
        return deletedCount > 0;
    }
}
