package com.siyan1234.itproject2nd.stock.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockApiItemDto {
    private String basDt;
    private String srtnCd;   // 단축코드
    private String itmsNm;   // 종목명
    private String clpr;     // 종가
    private String vs;       // 전일 대비
    private String fltRt;    // 등락률
    private String mkp;      // 시가
    private String hipr;     // 고가
    private String lopr;     // 저가
}
