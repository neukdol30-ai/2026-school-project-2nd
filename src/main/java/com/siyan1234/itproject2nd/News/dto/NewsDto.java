package com.siyan1234.itproject2nd.News.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NewsDto {
    private int id;
    private String title;
    private String summary;
    private String source;
    private String publishedAt;
    private String link;
    private String thumbnailUrl;

}