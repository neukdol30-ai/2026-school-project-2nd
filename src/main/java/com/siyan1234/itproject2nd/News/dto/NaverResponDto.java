package com.siyan1234.itproject2nd.News.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class NaverResponDto {
    private List<NaverNewsItemDto> items;
}
