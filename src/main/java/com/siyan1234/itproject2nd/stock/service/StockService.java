package com.siyan1234.itproject2nd.stock.service;

import com.siyan1234.itproject2nd.stock.dao.StockDao;
import com.siyan1234.itproject2nd.stock.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.lang.Long.parseLong;
import static org.apache.tomcat.util.http.FastHttpDateFormat.formatDate;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockDao stockDao;

    @Value("${data-go.service-key}")
    private String serviceKey;

    public List<StockDto> getMainStocks(){
        try{
            List<String> symbols = List.of(
                    "005930", // 삼성전자
                    "000660", // SK하이닉스
                    "207940", // 삼성바이오로직스
                    "373220", // LG에너지솔루션
                    "005380", // 현대차
                    "068270", // 셀트리온
                    "000270", // 기아
                    "035420", // NAVER
                    "105560", // KB금융
                    "009150"  // 삼성전기
            );

            return symbols.stream()
                    .map(this::getStockFromApi)
                    .toList();
        } catch(Exception e){
            return stockDao.findMainStocks()
                    .stream()
                    .peek(stock -> stock.setPoints(makePoints(stock.getPrice(), stock.getChangePrice())))
                    .toList();
        }
    }

    private StockDto getStockFromApi(String symbol) {
        String url = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo"
                + "?serviceKey=" + serviceKey
                + "&resultType=json"
                + "&numOfRows=30"
                + "&pageNo=1"
                + "&likeSrtnCd=" + UriUtils.encode(symbol, StandardCharsets.UTF_8);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<StockApiResponseDto> response = restTemplate.getForEntity(
                url,
                StockApiResponseDto.class
        );

        List<StockApiItemDto> items = response.getBody()
                .getResponse()
                .getBody()
                .getItems()
                .getItem();

        StockApiItemDto latestItem = items.get(0);
        long price = parseLong(latestItem.getClpr());
        long changePrice = parseLong(latestItem.getVs());
        double changeRate = parseDouble(latestItem.getFltRt());

        List<StockPointDto> points = items.stream()
                .sorted((a, b) -> a.getBasDt().compareTo(b.getBasDt()))
                .map(item -> new StockPointDto(
                        formatDate(item.getBasDt()),
                        parseLong(item.getClpr())
                ))
                .toList();

        return new StockDto(
                0,
                latestItem.getSrtnCd(),
                latestItem.getItmsNm(),
                price,
                changePrice,
                changeRate,
                points
        );
    }

    private List<StockPointDto> makePoints(long price, long changePrice) {
        long start = price - changePrice;

        return List.of(
                new StockPointDto("시가", start),
                new StockPointDto("저가", Math.min(start, price) - Math.abs(changePrice / 2)),
                new StockPointDto("고가", Math.max(start, price) + Math.abs(changePrice / 2)),
                new StockPointDto("현재", price)
        );
    }

    private long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Long.parseLong(value.replace(",", ""));
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        return Double.parseDouble(value.replace(",", ""));
    }

    private String formatDate(String basDt) {
        if (basDt == null || basDt.length() != 8) {
            return "";
        }

        return basDt.substring(4, 6) + "/" + basDt.substring(6, 8);
    }
}
