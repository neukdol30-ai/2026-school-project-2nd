package com.siyan1234.itproject2nd.News.dao;

import com.siyan1234.itproject2nd.News.dto.NewsDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface NewsDao {
    List<NewsDto> findMainNews();
}
