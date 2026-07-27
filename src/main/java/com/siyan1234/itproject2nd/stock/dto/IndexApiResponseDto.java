package com.siyan1234.itproject2nd.stock.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class IndexApiResponseDto {
    private ResponseDto response;

    @Getter
    @Setter
    public static class ResponseDto {
        private BodyDto body;
    }

    @Getter
    @Setter
    public static class BodyDto {
        private ItemsDto items;
    }

    @Getter
    @Setter
    public static class ItemsDto {
        private List<IndexItemDto> item;
    }

    @Getter
    @Setter
    public static class IndexItemDto {
        private String basDt;  // 기준일
        private String idxNm;  // 지수명: 코스피, 코스닥
        private String clpr;   // 종가
        private String vs;     // 전일 대비
        private String fltRt;  // 등락률
    }
}
