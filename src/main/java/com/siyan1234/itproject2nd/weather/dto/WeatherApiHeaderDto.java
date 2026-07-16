package com.siyan1234.itproject2nd.weather.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class WeatherApiHeaderDto {
    private String resultCode;
    private String resultMsg;
}
