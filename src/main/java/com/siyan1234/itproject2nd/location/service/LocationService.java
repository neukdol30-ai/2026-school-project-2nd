package com.siyan1234.itproject2nd.location.service;

import com.siyan1234.itproject2nd.location.dto.LocationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LocationService {
    public LocationDto getDefaultLocation(){
        return new LocationDto(
                "서울 강남구",
                "서울특별시",
                "강남구",
                "서울특별시 강남구",
                61,
                126,
                "11B00000",
                "11B10101"
        );
    }
}
