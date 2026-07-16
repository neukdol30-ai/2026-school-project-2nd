package com.siyan1234.itproject2nd.weather.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class WeatherApiItemsDto {

    private List<WeatherApiItemDto> item;
}
