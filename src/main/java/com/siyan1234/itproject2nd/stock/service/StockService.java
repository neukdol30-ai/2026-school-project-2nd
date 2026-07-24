package com.siyan1234.itproject2nd.stock.service;

import com.siyan1234.itproject2nd.stock.dto.IndexApiResponseDto;
import com.siyan1234.itproject2nd.stock.dto.StockApiItemDto;
import com.siyan1234.itproject2nd.stock.dto.StockApiResponseDto;
import com.siyan1234.itproject2nd.stock.dto.StockDto;
import com.siyan1234.itproject2nd.stock.dto.StockPointDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${data-go.service-key}")
    private String serviceKey;

    public List<StockDto> getMainStocks() {
        List<StockDto> stocks = new ArrayList<>();

        stocks.add(getMarketIndexFromApi("코스피", "KOSPI"));
        stocks.add(getMarketIndexFromApi("코스닥", "KOSDAQ"));

        List<String> symbols = List.of(
                "005930", // 삼성전자
                "000660", // SK하이닉스
                "373220", // LG에너지솔루션
                "005380", // 현대차
                "000270", // 기아
                "035420", // NAVER
                "105560", // KB금융
                "009150"  // 삼성전기
        );

        stocks.addAll(
                symbols.stream()
                        .map(this::getStockFromApi)
                        .toList()
        );

        return stocks;
    }

    private StockDto getStockFromApi(String symbol) {
        URI uri = createApiUri(
                "https://apis.data.go.kr/1160100/service/"
                        + "GetStockSecuritiesInfoService/"
                        + "getStockPriceInfo",
                "likeSrtnCd",
                symbol
        );

        StockApiResponseDto response =
                restTemplate.getForObject(
                        uri,
                        StockApiResponseDto.class
                );

        List<StockApiItemDto> items = getStockItems(
                response,
                symbol
        );

        StockApiItemDto latestItem = items.get(0);

        List<StockPointDto> points = items.stream()
                .sorted((a, b) ->
                        a.getBasDt().compareTo(b.getBasDt())
                )
                .map(item -> new StockPointDto(
                        formatDate(item.getBasDt()),
                        parseDouble(item.getClpr())
                ))
                .toList();

        return new StockDto(
                0,
                latestItem.getSrtnCd(),
                latestItem.getItmsNm(),
                parseDouble(latestItem.getClpr()),
                parseDouble(latestItem.getVs()),
                parseDouble(latestItem.getFltRt()),
                points
        );
    }

    private StockDto getMarketIndexFromApi(
            String indexName,
            String indexCode
    ) {
        URI uri = createApiUri(
                "https://apis.data.go.kr/1160100/service/"
                        + "GetMarketIndexInfoService/"
                        + "getStockMarketIndex",
                "idxNm",
                indexName
        );

        IndexApiResponseDto response =
                restTemplate.getForObject(
                        uri,
                        IndexApiResponseDto.class
                );

        List<IndexApiResponseDto.IndexItemDto> items =
                getIndexItems(response, indexName);

        IndexApiResponseDto.IndexItemDto latestItem =
                items.get(0);

        List<StockPointDto> points = items.stream()
                .sorted((a, b) ->
                        a.getBasDt().compareTo(b.getBasDt())
                )
                .map(item -> new StockPointDto(
                        formatDate(item.getBasDt()),
                        parseDouble(item.getClpr())
                ))
                .toList();

        return new StockDto(
                0,
                indexCode,
                latestItem.getIdxNm(),
                parseDouble(latestItem.getClpr()),
                parseDouble(latestItem.getVs()),
                parseDouble(latestItem.getFltRt()),
                points
        );
    }

    private URI createApiUri(
            String endpoint,
            String filterName,
            String filterValue
    ) {
        String encodedValue = UriUtils.encode(
                filterValue,
                StandardCharsets.UTF_8
        );

        String url = endpoint
                + "?serviceKey=" + serviceKey
                + "&resultType=json"
                + "&numOfRows=30"
                + "&pageNo=1"
                + "&" + filterName + "=" + encodedValue;

        return URI.create(url);
    }

    private List<StockApiItemDto> getStockItems(
            StockApiResponseDto response,
            String symbol
    ) {
        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody()
                .getItems().getItem() == null
                || response.getResponse().getBody()
                .getItems().getItem().isEmpty()) {
            throw new IllegalStateException(
                    symbol + " 종목 데이터를 찾지 못했습니다."
            );
        }

        return response.getResponse()
                .getBody()
                .getItems()
                .getItem();
    }

    private List<IndexApiResponseDto.IndexItemDto> getIndexItems(
            IndexApiResponseDto response,
            String indexName
    ) {
        if (response == null
                || response.getResponse() == null
                || response.getResponse().getBody() == null
                || response.getResponse().getBody().getItems() == null
                || response.getResponse().getBody()
                .getItems().getItem() == null
                || response.getResponse().getBody()
                .getItems().getItem().isEmpty()) {
            throw new IllegalStateException(
                    indexName + " 지수 데이터를 찾지 못했습니다."
            );
        }

        return response.getResponse()
                .getBody()
                .getItems()
                .getItem();
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Double.parseDouble(
                value.replace(",", "")
        );
    }

    private String formatDate(String basDt) {
        if (basDt == null || basDt.length() != 8) {
            return "";
        }

        return basDt.substring(4, 6)
                + "/"
                + basDt.substring(6, 8);
    }
}