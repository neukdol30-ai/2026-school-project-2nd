package com.siyan1234.itproject2nd.weather.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WeatherApiItemDto {

    private String baseDate;
    private String baseTime;

    private String category;

    private String fcstDate;
    private String fcstTime;
    private String fcstValue;

    private int nx;
    private int ny;

    // 중기 육상예보: 3일 후 ~ 5일 후 날씨와 강수확률
    private String wf3Am;
    private String wf3Pm;
    private String wf4Am;
    private String wf4Pm;
    private String wf5Am;
    private String wf5Pm;

    private String rnSt3Am;
    private String rnSt3Pm;
    private String rnSt4Am;
    private String rnSt4Pm;
    private String rnSt5Am;
    private String rnSt5Pm;

    // 중기 기온예보: 3일 후 ~ 5일 후 최저·최고기온
    private String taMin3;
    private String taMax3;
    private String taMin4;
    private String taMax4;
    private String taMin5;
    private String taMax5;
}