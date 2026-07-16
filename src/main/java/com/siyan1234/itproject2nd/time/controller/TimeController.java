package com.siyan1234.itproject2nd.time.controller;

import com.siyan1234.itproject2nd.time.dto.SunTimeDto;
import com.siyan1234.itproject2nd.time.service.TimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/time")
public class TimeController {

    private final TimeService timeService;

    @GetMapping("/sun")
    public SunTimeDto getSunTime() {
        return timeService.getSunTime();
    }
}