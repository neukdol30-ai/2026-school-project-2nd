package com.siyan1234.itproject2nd.location.controller;


import com.siyan1234.itproject2nd.location.dto.LocationDto;
import com.siyan1234.itproject2nd.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/location")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/default")
    public LocationDto getDefaultLocation() {
        return locationService.getDefaultLocation();
    }
}
