package com.siyan1234.itproject2nd.location.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDto {

    //기본 지역 dto
    private String regionName;
    private String state;
    private String city;
    private String address;

    //좌표 dto
    private int nx;
    private int ny;

    //날씨 관련 dto
    private String midLandRegId;
    private String midTempRegId;
}
