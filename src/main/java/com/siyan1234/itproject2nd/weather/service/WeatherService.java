package com.siyan1234.itproject2nd.weather.service;

import com.siyan1234.itproject2nd.location.dto.LocationDto;
import com.siyan1234.itproject2nd.location.service.LocationService;
import com.siyan1234.itproject2nd.weather.dto.WeatherDto;
import com.siyan1234.itproject2nd.weather.dto.WeeklyWeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final LocationService locationService;

    public WeatherDto getWeather() {
        LocationDto location = locationService.getDefaultLocation();

        return new WeatherDto(
                location.getRegionName(),
                "24",
                "구름 조금",
                "20",
                List.of(
                        new WeeklyWeatherDto("월", "맑음", "22", "30", "10"),
                        new WeeklyWeatherDto("화", "구름", "23", "29", "20"),
                        new WeeklyWeatherDto("수", "비", "21", "26", "70"),
                        new WeeklyWeatherDto("목", "흐림", "22", "27", "40"),
                        new WeeklyWeatherDto("금", "맑음", "23", "31", "10")
                )
        );
    }
}