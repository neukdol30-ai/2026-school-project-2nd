package com.siyan1234.itproject2nd.service;

import com.siyan1234.itproject2nd.dto.NewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NewsService {

    public List<NewsResponse> getNews(String query) {
        return List.of(
                new NewsResponse(
                        "AI 산업 관련 주요 뉴스",
                        "AI 기술이 여러 산업 분야에 빠르게 적용되고 있다는 내용의 뉴스입니다.",
                        "https://example.com/news/1",
                        "테크뉴스",
                        "2026-07-07"
                ),
                new NewsResponse(
                        "오늘의 경제 뉴스 요약",
                        "환율과 금리 흐름이 시장에 영향을 주고 있다는 경제 뉴스 요약입니다.",
                        "https://example.com/news/2",
                        "경제신문",
                        "2026-07-07"
                ),
                new NewsResponse(
                        "개발자 채용 시장 동향",
                        "기업들의 개발자 채용 방식과 요구 역량이 변화하고 있다는 기사입니다.",
                        "https://example.com/news/3",
                        "IT데일리",
                        "2026-07-07"
                )
        );
    }
}
