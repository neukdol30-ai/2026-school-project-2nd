package com.siyan1234.itproject2nd.weather.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WeatherApiBodyDto {

    private String dataType;
    private WeatherApiItemsDto items;
    private int pageNo;
    private int numOfRows;
    private int totalCount;
}