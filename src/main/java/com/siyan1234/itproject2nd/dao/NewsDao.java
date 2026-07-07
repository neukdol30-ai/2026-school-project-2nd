package com.siyan1234.itproject2nd.dao;

import com.siyan1234.itproject2nd.dto.NewsDto;

import java.util.List;

public interface NewsDao {
    List<NewsDto> findMainNews();
}
