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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import com.siyan1234.itproject2nd.News.dto.NaverNewsItemDto;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;


@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsDao newsDao;

    @Value("${naver.client-id}")
    private String clientId;

    @Value("${naver.client-secret}")
    private String clientSecret;

    // 뉴스 결과에서 제외할 사이트 도메인
    private static final Set<String> BLOCKED_NEWS_SITES = Set.of(
            "allurekorea.com"
    );

    // 화면 카테고리별 실제 뉴스 검색어
    private static final Map<String, List<String>> CATEGORY_QUERIES = Map.of(
            "정치", List.of("국회", "대통령", "정당", "선거"),
            "경제", List.of("증시", "금리", "환율", "기업"),
            "엔터테인먼트", List.of("연예", "배우", "가수", "드라마"),
            "스포츠", List.of("축구", "야구", "농구", "배구"),
            "사회", List.of("사건", "사고", "법원", "경찰"),
            "해외", List.of("미국", "중국", "일본", "국제")

    );

    private static final Map<String, Set<String>> CATEGORY_KEYWORDS = Map.of(
            "정치", Set.of("국회", "대통령", "정당", "선거", "의원", "여당", "야당"),
            "경제", Set.of("증시", "주가", "코스피", "코스닥", "금리", "환율", "금융", "기업"),
            "엔터테인먼트", Set.of("연예", "배우", "가수", "드라마", "영화", "방송"),
            "스포츠", Set.of("축구", "야구", "농구", "배구", "선수", "리그", "대표팀"),
            "사회", Set.of("사건", "사고", "법원", "경찰", "재판", "화재", "교육"),
            "해외", Set.of("미국", "중국", "일본", "유럽", "러시아", "국제", "해외")
    );

    public List<NewsDto> getMainNews(String category) {
        if (clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {
            return List.of();
        }

        List<String> queries = CATEGORY_QUERIES.getOrDefault(
                category,
                List.of("주요 뉴스")
        );

        List<List<NaverNewsItemDto>> groups = queries.stream()
                .map(this::searchNaverNews)
                .toList();

        Set<String> seenLinks = new HashSet<>();
        Set<String> seenTitles = new HashSet<>();
        List<NewsDto> result = new ArrayList<>();

        int maxSize = groups.stream()
                .mapToInt(List::size)
                .max()
                .orElse(0);

        // 검색어별 결과를 번갈아 후보 목록에 담기
        List<NaverNewsItemDto> candidates = new ArrayList<>();

        for (int index = 0; index < maxSize; index++) {
            for (List<NaverNewsItemDto> group : groups) {
                if (index < group.size()) {
                    candidates.add(group.get(index));
                }
            }
        }

// 1차: 카테고리 키워드까지 일치하는 기사
// 2차: 부족한 수량을 검색 결과에서 보충
        for (int pass = 0;
             pass < 2 && result.size() < 6;
             pass++) {
            boolean strictCategory = pass == 0;

            for (NaverNewsItemDto item : candidates) {
                if (result.size() >= 6) {
                    break;
                }

                String title = cleanText(item.getTitle());
                String summary = cleanText(item.getDescription());

                String link = item.getOriginalLink();

                if (link == null || link.isBlank()) {
                    link = item.getLink();
                }

                if (link == null || isBlockedNewsSite(link)) {
                    continue;
                }

                // 1차 선별에서만 엄격한 카테고리 검사
                if (strictCategory
                        && !matchesCategory(category, title, summary)) {
                    continue;
                }

                String titleKey = normalizeTitle(title);

                // contains로 먼저 검사해야 한쪽 Set만 추가되는 문제를 막을 수 있음
                if (seenLinks.contains(link)
                        || seenTitles.contains(titleKey)) {
                    continue;
                }

                seenLinks.add(link);
                seenTitles.add(titleKey);

                result.add(new NewsDto(
                        result.size() + 1,
                        title,
                        summary,
                        "뉴스",
                        item.getPubDate(),
                        link,
                        findThumbnailUrl(link)
                ));
            }
        }

        System.out.println(
                "카테고리: " + category
                        + " / 최종 통과: " + result.size()
        );

        return result;
    }

    // 검색어 하나로 네이버 뉴스 후보 목록 조회
    private List<NaverNewsItemDto> searchNaverNews(
            String searchKeyword
    ) {
        try {
            String query = UriUtils.encode(
                    searchKeyword,
                    StandardCharsets.UTF_8
            );

            String url = "https://openapi.naver.com/v1/search/news.json"
                    + "?query=" + query
                    + "&display=40"
                    + "&start=1"
                    + "&sort=sim";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", clientId);
            headers.set("X-Naver-Client-Secret", clientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<NaverResponDto> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            entity,
                            NaverResponDto.class
                    );

            NaverResponDto body = response.getBody();

            if (body == null || body.getItems() == null) {
                return List.of();
            }

            System.out.println(
                    "뉴스 검색어: " + searchKeyword
                            + " / API 결과: " + body.getItems().size()
            );

            return body.getItems();



        } catch (Exception e) {
            System.out.println(
                    "뉴스 API 검색 실패: " + searchKeyword
                            + " / " + e.getMessage()
            );
            return List.of();
        }
    }

    private boolean matchesCategory(
            String category,
            String title,
            String summary
    ) {
        Set<String> keywords = CATEGORY_KEYWORDS.get(category);

        if (keywords == null || keywords.isEmpty()) {
            return true;
        }

        String text = title + " " + summary;

        return keywords.stream().anyMatch(text::contains);
    }

    private String normalizeTitle(String title) {
        String normalized = title
                .replaceAll("[^가-힣a-zA-Z0-9]", "")
                .toLowerCase();

        // 뒤에 언론사명만 다르게 붙은 중복 기사도 제거
        return normalized.length() > 35
                ? normalized.substring(0, 35)
                : normalized;
    }

    // 차단 사이트 기사인지 확인
    private boolean isBlockedNewsSite(String link) {
        if (link == null || link.isBlank()) {
            return false;
        }

        return BLOCKED_NEWS_SITES.stream()
                .anyMatch(site -> link.contains(site));
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

    // 기사 원문 페이지의 대표 이미지 주소(og:image) 조회
    private String findThumbnailUrl(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }

        try {
            Document document = Jsoup.connect(link)
                    .userAgent("Mozilla/5.0")
                    .timeout(3000)
                    .followRedirects(true)
                    .get();

            Element image = document.selectFirst(
                    "meta[property=og:image], meta[name=twitter:image]"
            );

            if (image == null) {
                return null;
            }

            String imageUrl = image.attr("content");

            return imageUrl.isBlank() ? null : imageUrl;

        } catch (Exception e) {
            return null;
        }
    }
}