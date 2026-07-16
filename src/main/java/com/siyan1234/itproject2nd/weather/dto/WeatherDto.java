package com.siyan1234.itproject2nd.weather.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDto {
    private String location;
    private String currentTemp;
    private String weatherText;
    private String rainPercent;
    private List<WeeklyWeatherDto> weekly;
}
