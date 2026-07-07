package com.siyan1234.itproject2nd.service;

import com.siyan1234.itproject2nd.dao.NewsDao;
import com.siyan1234.itproject2nd.dto.NewsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {
    private final NewsDao newsDao;
    public List<NewsDto> getMainNews(){
        return newsDao.findMainNews();
    }
}
