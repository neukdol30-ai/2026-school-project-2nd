package com.siyan1234.itproject2nd.weather.controller;

import com.siyan1234.itproject2nd.weather.dto.WeatherDto;
import com.siyan1234.itproject2nd.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/weather")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public WeatherDto getWeather(){
        return weatherService.getWeather();
    }
}
