package com.siyan1234.itproject2nd.News.controller;

import com.siyan1234.itproject2nd.News.dto.NewsDto;
import com.siyan1234.itproject2nd.News.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @GetMapping
    public List<NewsDto> getMainNews() {
        return newsService.getMainNews();
    }
}