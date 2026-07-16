package com.siyan1234.itproject2nd.weather.service;

import com.siyan1234.itproject2nd.location.dto.LocationDto;
import com.siyan1234.itproject2nd.location.service.LocationService;
import com.siyan1234.itproject2nd.weather.dto.WeatherApiItemDto;
import com.siyan1234.itproject2nd.weather.dto.WeatherApiResponseDto;
import com.siyan1234.itproject2nd.weather.dto.WeatherDto;
import com.siyan1234.itproject2nd.weather.dto.WeeklyWeatherDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WeatherService {

    private final LocationService locationService;

    @Value("${data-go.service-key}")
    private String serviceKey;

    public WeatherDto getWeather() {
        LocationDto location = locationService.getDefaultLocation();

        try {
            return getWeatherFromApi(location);
        } catch (Exception e) {
            return getDummyWeather(location);
        }
    }

    private WeatherDto getWeatherFromApi(LocationDto location) {
        LocalDateTime baseDateTime = getBaseDateTime();

        String baseDate = baseDateTime.format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        String baseTime = baseDateTime.format(
                DateTimeFormatter.ofPattern("HHmm")
        );

        String url = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getVilageFcst"
                + "?serviceKey=" + serviceKey
                + "&pageNo=1"
                + "&numOfRows=1000"
                + "&dataType=JSON"
                + "&base_date=" + baseDate
                + "&base_time=" + baseTime
                + "&nx=" + location.getNx()
                + "&ny=" + location.getNy();

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<WeatherApiResponseDto> response =
                restTemplate.getForEntity(url, WeatherApiResponseDto.class);

        List<WeatherApiItemDto> items = response.getBody()
                .getResponse()
                .getBody()
                .getItems()
                .getItem();

        WeatherApiItemDto tempItem = findTargetTemperature(items);

        String forecastDate = tempItem.getFcstDate();
        String forecastTime = tempItem.getFcstTime();

        String temp = tempItem.getFcstValue();

        String sky = getForecastValue(items, "SKY", forecastDate, forecastTime);
        String pty = getForecastValue(items, "PTY", forecastDate, forecastTime);
        String pop = getForecastValue(items, "POP", forecastDate, forecastTime);

        return new WeatherDto(
                location.getRegionName(),
                formatTemperature(temp),
                getWeatherText(sky, pty),
                pop,
                getWeeklyWeather(items, location)
        );
    }

    private LocalDateTime getBaseDateTime() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(30);

        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};

        for (int index = baseHours.length - 1; index >= 0; index--) {
            if (now.getHour() >= baseHours[index]) {
                return now.withHour(baseHours[index])
                        .withMinute(0)
                        .withSecond(0)
                        .withNano(0);
            }
        }

        return now.minusDays(1)
                .withHour(23)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }

    private WeatherApiItemDto findTargetTemperature(
            List<WeatherApiItemDto> items
    ) {
        return items.stream()
                .filter(item -> "TMP".equals(item.getCategory()))
                .min(Comparator.comparing(
                        item -> item.getFcstDate() + item.getFcstTime()
                ))
                .orElseThrow();
    }

    private String getForecastValue(
            List<WeatherApiItemDto> items,
            String category,
            String forecastDate,
            String forecastTime
    ) {
        return items.stream()
                .filter(item -> category.equals(item.getCategory()))
                .filter(item -> forecastDate.equals(item.getFcstDate()))
                .filter(item -> forecastTime.equals(item.getFcstTime()))
                .map(WeatherApiItemDto::getFcstValue)
                .findFirst()
                .orElse("0");
    }

    private String getWeatherText(String sky, String pty) {
        if ("1".equals(pty)) {
            return "비";
        }

        if ("2".equals(pty)) {
            return "비 또는 눈";
        }

        if ("3".equals(pty)) {
            return "눈";
        }

        if ("4".equals(pty)) {
            return "소나기";
        }

        if ("1".equals(sky)) {
            return "맑음";
        }

        if ("3".equals(sky)) {
            return "구름 많음";
        }

        return "흐림";
    }

    private WeatherDto getDummyWeather(LocationDto location) {
        return new WeatherDto(
                location.getRegionName(),
                "24",
                "구름 조금",
                "20",
                List.of(
                        new WeeklyWeatherDto("월", "맑음", "22", "30", "10"),
                        new WeeklyWeatherDto("화", "구름", "23", "29", "20"),
                        new WeeklyWeatherDto("수", "비", "21", "26", "70"),
                        new WeeklyWeatherDto("목", "흐림", "22", "27", "40"),
                        new WeeklyWeatherDto("금", "맑음", "23", "31", "10")
                )
        );
    }

    private WeatherApiItemDto getMidLandForecast(LocationDto location) {
        String url = "https://apis.data.go.kr/1360000/MidFcstInfoService/getMidLandFcst"
                + "?serviceKey=" + serviceKey
                + "&pageNo=1"
                + "&numOfRows=10"
                + "&dataType=JSON"
                + "&regId=" + location.getMidLandRegId()
                + "&tmFc=" + getMidForecastTime();

        return requestMidForecast(url);
    }

    private WeatherApiItemDto getMidTemperatureForecast(LocationDto location) {
        String url = "https://apis.data.go.kr/1360000/MidFcstInfoService/getMidTa"
                + "?serviceKey=" + serviceKey
                + "&pageNo=1"
                + "&numOfRows=10"
                + "&dataType=JSON"
                + "&regId=" + location.getMidTempRegId()
                + "&tmFc=" + getMidForecastTime();

        return requestMidForecast(url);
    }

    private WeatherApiItemDto requestMidForecast(String url) {
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<WeatherApiResponseDto> response =
                restTemplate.getForEntity(url, WeatherApiResponseDto.class);

        return response.getBody()
                .getResponse()
                .getBody()
                .getItems()
                .getItem()
                .stream()
                .findFirst()
                .orElseThrow();
    }

    private String getMidForecastTime() {
        LocalDateTime now = LocalDateTime.now().minusMinutes(40);

        LocalDateTime forecastTime;

        if (now.getHour() >= 18) {
            forecastTime = now.withHour(18);
        } else if (now.getHour() >= 6) {
            forecastTime = now.withHour(6);
        } else {
            forecastTime = now.minusDays(1).withHour(18);
        }

        return forecastTime
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private List<WeeklyWeatherDto> getWeeklyWeather(
            List<WeatherApiItemDto> shortItems,
            LocationDto location
    ) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        WeatherApiItemDto midLand = getMidLandForecast(location);
        WeatherApiItemDto midTemp = getMidTemperatureForecast(location);

        return List.of(
                makeShortWeeklyWeather(shortItems, tomorrow),
                makeShortWeeklyWeather(shortItems, tomorrow.plusDays(1)),
                makeShortWeeklyWeather(shortItems, tomorrow.plusDays(2)),

                makeMidWeeklyWeather(
                        tomorrow.plusDays(3),
                        midLand.getWf4Am(),
                        midLand.getWf4Pm(),
                        midLand.getRnSt4Am(),
                        midLand.getRnSt4Pm(),
                        midTemp.getTaMin4(),
                        midTemp.getTaMax4()
                ),

                makeMidWeeklyWeather(
                        tomorrow.plusDays(4),
                        midLand.getWf5Am(),
                        midLand.getWf5Pm(),
                        midLand.getRnSt5Am(),
                        midLand.getRnSt5Pm(),
                        midTemp.getTaMin5(),
                        midTemp.getTaMax5()
                )
        );
    }

    private WeeklyWeatherDto makeShortWeeklyWeather(
            List<WeatherApiItemDto> items,
            LocalDate date
    ) {
        String forecastDate = date.format(
                DateTimeFormatter.ofPattern("yyyyMMdd")
        );

        String sky = getDailyForecastValue(items, "SKY", forecastDate);
        String pty = getDailyForecastValue(items, "PTY", forecastDate);

        return new WeeklyWeatherDto(
                getDayName(date),
                getWeatherText(sky, pty),
                formatTemperature(getDailyForecastValue(items, "TMN", forecastDate)),
                formatTemperature(getDailyForecastValue(items, "TMX", forecastDate)),
                getDailyRainPercent(items, forecastDate)
        );
    }

    private WeeklyWeatherDto makeMidWeeklyWeather(
            LocalDate date,
            String weatherAm,
            String weatherPm,
            String rainAm,
            String rainPm,
            String minTemp,
            String maxTemp
    ) {
        return new WeeklyWeatherDto(
                getDayName(date),
                getMidWeatherText(weatherAm, weatherPm),
                formatTemperature(minTemp),
                formatTemperature(maxTemp),
                String.valueOf(
                        Math.max(parseWeatherNumber(rainAm),
                                parseWeatherNumber(rainPm))
                )
        );
    }

    private String getDailyForecastValue(
            List<WeatherApiItemDto> items,
            String category,
            String forecastDate
    ) {
        return items.stream()
                .filter(item -> category.equals(item.getCategory()))
                .filter(item -> forecastDate.equals(item.getFcstDate()))
                .map(WeatherApiItemDto::getFcstValue)
                .findFirst()
                .orElse("-");
    }

    private String getDailyRainPercent(
            List<WeatherApiItemDto> items,
            String forecastDate
    ) {
        int maxRain = items.stream()
                .filter(item -> "POP".equals(item.getCategory()))
                .filter(item -> forecastDate.equals(item.getFcstDate()))
                .mapToInt(item -> parseWeatherNumber(item.getFcstValue()))
                .max()
                .orElse(0);

        return String.valueOf(maxRain);
    }

    private String getMidWeatherText(String weatherAm, String weatherPm) {
        if (weatherAm == null || weatherAm.isBlank()) {
            return weatherPm;
        }

        if (weatherPm == null || weatherPm.isBlank()) {
            return weatherAm;
        }

        if (weatherAm.equals(weatherPm)) {
            return weatherAm;
        }

        return weatherAm + " / " + weatherPm;
    }

    private int parseWeatherNumber(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return 0;
        }

        return Integer.parseInt(value);
    }

    private String getDayName(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
    }

    private String formatTemperature(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }

        if (value.endsWith(".0")) {
            return value.substring(0, value.length() - 2);
        }

        return value;
    }
}