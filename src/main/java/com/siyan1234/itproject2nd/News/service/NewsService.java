package com.siyan1234.itproject2nd.News.service;

import com.siyan1234.itproject2nd.News.dao.NewsDao;
import com.siyan1234.itproject2nd.News.dto.NaverResponDto;
import com.siyan1234.itproject2nd.News.dto.NewsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsDao newsDao;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    public List<NewsDto> getMainNews() {
        try {
            //공백 대비 코드
            if (clientId == null || clientId.isBlank()
                    || clientSecret == null || clientSecret.isBlank()) {
                return newsDao.findMainNews();
            }
            String query = UriUtils.encode("IT", StandardCharsets.UTF_8);

            String url = "https://openapi.naver.com/v1/search/news.json"
                    + "?query=" + query
                    + "&display=5"
                    + "&start=1"
                    + "&sort=date";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<NaverResponDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    NaverResponDto.class
            );

            NaverResponDto naverResponse = response.getBody();

            if (naverResponse == null || naverResponse.getItems() == null) {
                return newsDao.findMainNews();
            }



            return IntStream.range(0, naverResponse.getItems().size())
                    .mapToObj(index -> {
                        var item = naverResponse.getItems().get(index);

                        String link = item.getOriginalLink();

                        if(link == null || link.isBlank()){
                            link = item.getLink();
                        }

                        return new NewsDto(
                                index + 1,
                                cleanText(item.getTitle()),
                                cleanText(item.getDescription()),
                                "뉴스",
                                item.getPubDate(),
                                link
                        );
                    })
                    .toList();

        } catch (Exception e) {
            return newsDao.findMainNews();
        }
    }

    //특수문자 대비 코드
    private String cleanText(String value){
        if (value == null){
            return "";
        }

        return value
                .replaceAll("<[^>]*>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&it;", "<")
                .replace("&gt;", ">")
                .replace("&#39;", "'");
    }
}