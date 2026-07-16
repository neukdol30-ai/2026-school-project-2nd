package com.siyan1234.itproject2nd.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyWeatherDto {
    private String day;
    private String weatherText;
    private String minTemp;
    private String maxTemp;
    private String rainPercent;
}
