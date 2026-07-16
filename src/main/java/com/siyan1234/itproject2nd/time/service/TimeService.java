package com.siyan1234.itproject2nd.time.service;

import com.siyan1234.itproject2nd.location.dto.LocationDto;
import com.siyan1234.itproject2nd.location.service.LocationService;
import com.siyan1234.itproject2nd.time.dto.SunTimeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimeService {

    private final LocationService locationService;

    @Value("${data-go.service-key}")
    private String serviceKey;

    public SunTimeDto getSunTime() {
        LocationDto location = locationService.getDefaultLocation();

        try {
            String locdate = LocalDate.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String url = "http://apis.data.go.kr/B090041/openapi/service/"
                    + "RiseSetInfoService/getAreaRiseSetInfo"
                    + "?serviceKey=" + serviceKey
                    + "&locdate=" + locdate
                    + "&location=" + getSunLocation(location)
                    + "&numOfRows=10"
                    + "&pageNo=1";

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response =
                    restTemplate.getForEntity(url, Map.class);

            Map<?, ?> root = response.getBody();

            if (root == null) {
                throw new IllegalStateException("출몰시각 API 응답이 비어 있습니다.");
            }

            Map<?, ?> responseData = getNestedMap(root, "response");
            Map<?, ?> body = getNestedMap(responseData, "body");
            Map<?, ?> items = getNestedMap(body, "items");
            Map<?, ?> item = getNestedMap(items, "item");

            String sunrise = getStringValue(item, "sunrise");
            String sunset = getStringValue(item, "sunset");

            return new SunTimeDto(
                    formatSunTime(sunrise),
                    formatSunTime(sunset)
            );

        } catch (Exception e) {
            return new SunTimeDto("--:--", "--:--");
        }
    }

    private String getSunLocation(LocationDto location) {
        if (location.getState().contains("서울")) {
            return "서울";
        }

        return location.getCity();
    }

    private Map<?, ?> getNestedMap(
            Map<?, ?> source,
            String key
    ) {
        Object value = source.get(key);

        if (!(value instanceof Map)) {
            throw new IllegalStateException(
                    key + " 데이터를 찾을 수 없습니다."
            );
        }

        return (Map<?, ?>) value;
    }

    private String getStringValue(
            Map<?, ?> source,
            String key
    ) {
        Object value = source.get(key);

        if (value == null) {
            return "";
        }

        return value.toString().trim();
    }

    private String formatSunTime(String value) {
        if (value == null || value.length() != 4) {
            return "--:--";
        }

        return value.substring(0, 2)
                + ":"
                + value.substring(2, 4);
    }
}