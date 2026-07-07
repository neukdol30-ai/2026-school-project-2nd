package com.siyan1234.itproject2nd.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsResponse {
    String title;
    String description;
    String link;
    String source;
    String pubDate;
}
