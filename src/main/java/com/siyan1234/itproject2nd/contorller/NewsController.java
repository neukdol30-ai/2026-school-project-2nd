package com.siyan1234.itproject2nd.contorller;


import com.siyan1234.itproject2nd.dto.NewsDto;
import com.siyan1234.itproject2nd.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/board")
@RequiredArgsConstructor
@Slf4j
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/api/news")
    public List<NewsDto> getMainNews(){
        return newsService.getMainNews();
    }
}
