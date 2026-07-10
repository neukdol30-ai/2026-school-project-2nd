package com.siyan1234.itproject2nd.News.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NaverNewsItemDto {
    private String title;
    private String originalLink;
    private String link;
    private String description;
    private String pubDate;
}
