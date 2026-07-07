package com.siyan1234.itproject2nd.News.dao;

import com.siyan1234.itproject2nd.News.dto.NewsDto;

import java.util.List;

public interface NewsDao {
    List<NewsDto> findMainNews();
}
