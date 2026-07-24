package com.siyan1234.itproject2nd.calendar.service;

import com.siyan1234.itproject2nd.calendar.dao.CalendarDao;
import com.siyan1234.itproject2nd.calendar.dao.GoogleCalendarTokenDao;
import com.siyan1234.itproject2nd.calendar.dto.CalendarEventDto;
import com.siyan1234.itproject2nd.calendar.dto.GoogleCalendarTokenDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    // =========================
    // 대한민국 휴일 캘린더 설정
    // 역할: 구글이 제공하는 대한민국 휴일 캘린더를 읽기 전용으로 조회
    // =========================
    private static final String KOREA_HOLIDAY_CALENDAR_ID =
            "ko.south_korea#holiday@group.v.calendar.google.com";

    // 휴일 조회 날짜와 시간을 대한민국 시간 기준으로 처리
    private static final ZoneId KOREA_TIME_ZONE =
            ZoneId.of("Asia/Seoul");

    private final GoogleCalendarTokenDao googleCalendarTokenDao;
    private final CalendarDao calendarDao;

    @Value("${google.calendar.client-id}")
    private String clientId;

    @Value("${google.calendar.client-secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri}")
    private String redirectUri;

    @Value("${google.calendar.scope}")
    private String scope;

    @Value("${google.calendar.holiday-api-key}")
    private String holidayApiKey;


    // =========================
    // 구글 권한 동의 URL 생성
    // =========================
    public String createGoogleAuthUrl(String state) {

        return "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(scope)
                + "&access_type=offline"
                + "&prompt=consent"
                + "&state=" + encode(state);
    }


    // =========================
    // 구글 캘린더 연동 여부 확인
    // 역할: 개인 일정 기능을 사용할 수 있는 회원인지 확인
    // =========================
    public boolean isGoogleCalendarConnected(Integer memberNo) {

        if (memberNo == null) {
            return false;
        }

        GoogleCalendarTokenDto tokenDto =
                googleCalendarTokenDao.findByMemberNo(memberNo);

        return tokenDto != null
                && tokenDto.getRefreshToken() != null
                && !tokenDto.getRefreshToken().isBlank();
    }


    // =========================
    // 구글 토큰 저장
    // =========================
    @Transactional
    public void saveGoogleToken(String code, Integer memberNo) {

        String tokenUrl = "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<Map> response =
                restTemplate.postForEntity(tokenUrl, request, Map.class);

        Map<String, Object> body = response.getBody();

        if (body == null) {
            throw new IllegalStateException(
                    "구글 토큰 응답이 비어 있습니다."
            );
        }

        String accessToken =
                (String) body.get("access_token");

        String refreshToken =
                (String) body.get("refresh_token");

        Number expiresIn =
                (Number) body.get("expires_in");

        if (accessToken == null) {
            throw new IllegalStateException(
                    "구글 access_token이 없습니다."
            );
        }

        if (expiresIn == null) {
            throw new IllegalStateException(
                    "구글 토큰 만료 시간이 없습니다."
            );
        }

        LocalDateTime tokenExpiry =
                LocalDateTime.now()
                        .plusSeconds(expiresIn.longValue());

        GoogleCalendarTokenDto oldToken =
                googleCalendarTokenDao.findByMemberNo(memberNo);

        if (refreshToken == null && oldToken != null) {
            refreshToken = oldToken.getRefreshToken();
        }

        GoogleCalendarTokenDto tokenDto =
                new GoogleCalendarTokenDto();

        tokenDto.setMemberNo(memberNo);
        tokenDto.setAccessToken(accessToken);
        tokenDto.setRefreshToken(refreshToken);
        tokenDto.setTokenExpiry(tokenExpiry);

        if (oldToken != null) {
            tokenDto.setGoogleEmail(
                    oldToken.getGoogleEmail()
            );
        } else {
            tokenDto.setGoogleEmail(null);
        }

        if (oldToken == null) {
            googleCalendarTokenDao.insertToken(tokenDto);
            return;
        }

        googleCalendarTokenDao.updateToken(tokenDto);
    }


    // =========================
    // access token 재발급
    // =========================
    @Transactional
    public String refreshAccessToken(Integer memberNo) {

        GoogleCalendarTokenDto oldToken =
                googleCalendarTokenDao.findByMemberNo(memberNo);

        if (oldToken == null
                || oldToken.getRefreshToken() == null) {

            throw new IllegalStateException(
                    "구글 캘린더 연동 정보가 없습니다."
            );
        }

        String tokenUrl =
                "https://oauth2.googleapis.com/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        MultiValueMap<String, String> params =
                new LinkedMultiValueMap<>();

        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add(
                "refresh_token",
                oldToken.getRefreshToken()
        );
        params.add("grant_type", "refresh_token");

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        RestTemplate restTemplate =
                new RestTemplate();

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        tokenUrl,
                        request,
                        Map.class
                );

        Map<String, Object> body =
                response.getBody();

        if (body == null) {
            throw new IllegalStateException(
                    "구글 토큰 재발급 응답이 비어 있습니다."
            );
        }

        String accessToken =
                (String) body.get("access_token");

        Number expiresIn =
                (Number) body.get("expires_in");

        if (accessToken == null || expiresIn == null) {
            throw new IllegalStateException(
                    "구글 access_token 재발급에 실패했습니다."
            );
        }

        LocalDateTime tokenExpiry =
                LocalDateTime.now()
                        .plusSeconds(expiresIn.longValue());

        GoogleCalendarTokenDto tokenDto =
                new GoogleCalendarTokenDto();

        tokenDto.setMemberNo(memberNo);
        tokenDto.setAccessToken(accessToken);
        tokenDto.setRefreshToken(
                oldToken.getRefreshToken()
        );
        tokenDto.setTokenExpiry(tokenExpiry);
        tokenDto.setGoogleEmail(
                oldToken.getGoogleEmail()
        );

        googleCalendarTokenDao.updateToken(tokenDto);

        return accessToken;
    }

    // 사용 가능한 토큰이면 기존 토큰 사용
    private String getValidAccessToken(
            Integer memberNo
    ) {

        GoogleCalendarTokenDto tokenDto =
                googleCalendarTokenDao
                        .findByMemberNo(memberNo);

        if (tokenDto == null) {
            throw new IllegalStateException(
                    "구글 캘린더 연동 정보가 없습니다."
            );
        }

        String accessToken =
                tokenDto.getAccessToken();

        LocalDateTime tokenExpiry =
                tokenDto.getTokenExpiry();

        // 만료까지 1분 이상 남았으면 기존 토큰 사용
        if (
                accessToken != null
                        && !accessToken.isBlank()
                        && tokenExpiry != null
                        && tokenExpiry.isAfter(
                        LocalDateTime.now()
                                .plusMinutes(1)
                )
        ) {
            return accessToken;
        }

        // 만료됐으면 새 토큰 발급
        return refreshAccessToken(memberNo);
    }




    // =========================
// 구글 → 우리 사이트 동기화
// =========================
    @Transactional
    @SuppressWarnings("unchecked")
    public void syncGoogleCalendarEvents(Integer memberNo) {

        String accessToken =
                getValidAccessToken(memberNo);

        LocalDate syncStartDate =
                LocalDate.now().minusDays(31);

        LocalDate syncEndDate =
                LocalDate.now().plusDays(366);

        String timeMin =
                Instant.now()
                        .minus(31, ChronoUnit.DAYS)
                        .truncatedTo(ChronoUnit.SECONDS)
                        .toString();

        String timeMax =
                Instant.now()
                        .plus(365, ChronoUnit.DAYS)
                        .truncatedTo(ChronoUnit.SECONDS)
                        .toString();

        String eventsUrl =
                UriComponentsBuilder
                        .fromUriString(
                                "https://www.googleapis.com/calendar/v3/calendars/primary/events"
                        )
                        .queryParam("singleEvents", "true")
                        .queryParam("orderBy", "startTime")
                        .queryParam("maxResults", "250")
                        .queryParam("timeMin", timeMin)
                        .queryParam("timeMax", timeMax)
                        .encode()
                        .toUriString();

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        RestTemplate restTemplate =
                new RestTemplate();

        ResponseEntity<Map> response =
                restTemplate.exchange(
                        eventsUrl,
                        HttpMethod.GET,
                        request,
                        Map.class
                );

        Map<String, Object> body =
                response.getBody();

        if (body == null) {

            throw new IllegalStateException(
                    "구글 일정 응답이 비어 있습니다."
            );
        }

        List<Map<String, Object>> items =
                (List<Map<String, Object>>) body.get(
                        "items"
                );

        List<String> googleEventIdList =
                new ArrayList<>();


        if (items != null) {

            for (Map<String, Object> item : items) {

                String googleEventId =
                        (String) item.get(
                                "id"
                        );

                String title =
                        (String) item.getOrDefault(
                                "summary",
                                "제목 없음"
                        );

                String content =
                        (String) item.get(
                                "description"
                        );

                String location =
                        (String) item.get(
                                "location"
                        );

                String updated =
                        (String) item.get(
                                "updated"
                        );

                Map<String, Object> start =
                        (Map<String, Object>) item.get(
                                "start"
                        );

                Map<String, Object> end =
                        (Map<String, Object>) item.get(
                                "end"
                        );


                // 일정 ID나 날짜 정보가 없으면 건너뜀
                if (
                        googleEventId == null
                                || start == null
                                || end == null
                ) {
                    continue;
                }


                googleEventIdList.add(
                        googleEventId
                );


                String startDatetime =
                        convertGoogleDateTime(
                                start,
                                true
                        );

                String endDatetime =
                        convertGoogleDateTime(
                                end,
                                false
                        );

                String allDayYn =
                        start.containsKey(
                                "date"
                        )
                                ? "Y"
                                : "N";

                String eventDate =
                        startDatetime.substring(
                                0,
                                10
                        );


                CalendarEventDto eventDto =
                        new CalendarEventDto();

                eventDto.setMemberNo(
                        memberNo
                );

                eventDto.setTitle(
                        title
                );

                eventDto.setContent(
                        content
                );

                eventDto.setLocation(
                        location
                );

                eventDto.setEventDate(
                        eventDate
                );

                eventDto.setStartDatetime(
                        startDatetime
                );

                eventDto.setEndDatetime(
                        endDatetime
                );

                eventDto.setAllDayYn(
                        allDayYn
                );

                eventDto.setSourceType(
                        "GOOGLE"
                );

                eventDto.setGoogleEventId(
                        googleEventId
                );

                eventDto.setGoogleCalendarId(
                        "primary"
                );

                eventDto.setGoogleUpdatedDatetime(
                        convertGoogleUpdatedDatetime(
                                updated
                        )
                );

                eventDto.setIsDeleted(
                        "N"
                );


                CalendarEventDto oldEvent =
                        calendarDao.findByGoogleEventId(
                                memberNo,
                                googleEventId
                        );


            /*
                DB에 없는 일정이면 새로 저장한다.
            */
                if (oldEvent == null) {

                    calendarDao.insertGoogleEvent(
                            eventDto
                    );

            /*
                구글의 실제 수정 시간이 달라진 경우에만
                DB 일정을 수정한다.

                5초 자동 동기화만으로 UPDATEDATE가
                계속 바뀌는 현상을 막는다.
            */
                } else if (
                        !java.util.Objects.equals(
                                oldEvent.getGoogleUpdatedDatetime(),
                                eventDto.getGoogleUpdatedDatetime()
                        )
                ) {

                    calendarDao.updateGoogleEvent(
                            eventDto
                    );
                }
            }
        }


    /*
        구글에서 삭제된 일정은
        사이트 DB에서도 삭제 상태로 변경한다.
    */
        calendarDao.deleteMissingGoogleEvents(
                memberNo,
                googleEventIdList,
                syncStartDate.toString(),
                syncEndDate.toString()
        );
    }


    // =========================
    // 대한민국 휴일 일정 조회
    // 역할: 사용자 구글 연동과 관계없이 API Key로 공휴일 조회
    // DB 저장, 구글 재등록, 수정, 삭제는 하지 않음
    // =========================
    @SuppressWarnings("unchecked")
    public List<CalendarEventDto> getKoreaHolidayEvents(
            Integer memberNo,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<CalendarEventDto> holidayEventList =
                new ArrayList<>();

        /*
            memberNo는 화면 표시용 DTO에만 들어간다.
            공휴일 조회 인증에는 사용자 토큰을 사용하지 않는다.
        */
        if (memberNo == null
                || startDate == null
                || endDate == null) {

            return holidayEventList;
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "휴일 조회 종료 날짜는 시작 날짜보다 빠를 수 없습니다."
            );
        }

        if (holidayApiKey == null
                || holidayApiKey.isBlank()) {

            throw new IllegalStateException(
                    "구글 공휴일 조회용 API Key가 설정되지 않았습니다."
            );
        }

        String timeMin = startDate
                .atStartOfDay(KOREA_TIME_ZONE)
                .toInstant()
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();

        String timeMax = endDate
                .plusDays(1)
                .atStartOfDay(KOREA_TIME_ZONE)
                .toInstant()
                .truncatedTo(ChronoUnit.SECONDS)
                .toString();

        RestTemplate restTemplate =
                new RestTemplate();

        String nextPageToken = null;

        do {
            URI eventsUri =
                    createKoreaHolidayEventsUri(
                            timeMin,
                            timeMax,
                            nextPageToken
                    );

            /*
                개인 access token을 넣지 않는다.
                공개 캘린더와 API Key만 사용한다.
            */
            ResponseEntity<Map> response =
                    restTemplate.getForEntity(
                            eventsUri,
                            Map.class
                    );

            Map<String, Object> body =
                    response.getBody();

            if (body == null) {
                throw new IllegalStateException(
                        "대한민국 휴일 일정 응답이 비어 있습니다."
                );
            }

            List<Map<String, Object>> items =
                    (List<Map<String, Object>>) body.get("items");

            if (items != null) {
                for (Map<String, Object> item : items) {

                    String status =
                            (String) item.get("status");

                    if ("cancelled".equals(status)) {
                        continue;
                    }

                    String googleEventId =
                            (String) item.get("id");

                    String title =
                            (String) item.getOrDefault(
                                    "summary",
                                    "휴일"
                            );

                    String content =
                            (String) item.get("description");

                    String location =
                            (String) item.get("location");

                    String updated =
                            (String) item.get("updated");

                    Map<String, Object> start =
                            (Map<String, Object>) item.get("start");

                    Map<String, Object> end =
                            (Map<String, Object>) item.get("end");

                    if (start == null || end == null) {
                        continue;
                    }

                    String startDatetime =
                            convertGoogleDateTime(
                                    start,
                                    true
                            );

                    String endDatetime =
                            convertGoogleDateTime(
                                    end,
                                    false
                            );

                    String allDayYn =
                            start.containsKey("date")
                                    ? "Y"
                                    : "N";

                    String eventDate =
                            startDatetime.substring(0, 10);

                    CalendarEventDto holidayDto =
                            new CalendarEventDto();

                    holidayDto.setMemberNo(memberNo);
                    holidayDto.setTitle(title);
                    holidayDto.setContent(content);
                    holidayDto.setLocation(location);
                    holidayDto.setEventDate(eventDate);
                    holidayDto.setStartDatetime(startDatetime);
                    holidayDto.setEndDatetime(endDatetime);
                    holidayDto.setAllDayYn(allDayYn);

                    /*
                        개인 구글 일정과 공휴일을 구분한다.
                        공휴일은 DB에 저장하지 않는다.
                    */
                    holidayDto.setSourceType(
                            "GOOGLE_HOLIDAY"
                    );

                    holidayDto.setGoogleEventId(
                            googleEventId
                    );

                    holidayDto.setGoogleCalendarId(
                            KOREA_HOLIDAY_CALENDAR_ID
                    );

                    holidayDto.setGoogleUpdatedDatetime(
                            convertGoogleUpdatedDatetime(updated)
                    );

                    holidayDto.setIsDeleted("N");

                    holidayEventList.add(holidayDto);
                }
            }

            nextPageToken =
                    (String) body.get("nextPageToken");

        } while (
                nextPageToken != null
                        && !nextPageToken.isBlank()
        );

        return holidayEventList;
    }


    // =========================
    // 우리 사이트 → 구글 일정 생성
    // =========================
    @Transactional
    public String createGoogleEvent(
            Integer memberNo,
            CalendarEventDto calendarEventDto
    ) {

        GoogleCalendarTokenDto tokenDto =
                googleCalendarTokenDao.findByMemberNo(memberNo);

        if (tokenDto == null
                || tokenDto.getRefreshToken() == null) {

            return null;
        }

        String accessToken =
                getValidAccessToken(memberNo);

        String eventDate =
                calendarEventDto.getEventDate();

        if (eventDate == null || eventDate.isBlank()) {
            return null;
        }

        String title =
                calendarEventDto.getTitle();

        String content =
                calendarEventDto.getContent();

        String location =
                calendarEventDto.getLocation();

        if (title == null || title.isBlank()) {
            title = "제목 없음";
        }

        String eventsInsertUrl =
                "https://www.googleapis.com/calendar/v3/calendars/primary/events";

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);
        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        Map<String, Object> start;
        Map<String, Object> end;

/*
    시간이 있는 일정은 dateTime으로 전송하고,
    시간이 없으면 날짜 일정으로 전송한다.
*/
        if ("N".equals(
                calendarEventDto.getAllDayYn()
        )) {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm"
                    );

            LocalDateTime startDateTime =
                    LocalDateTime.parse(
                            calendarEventDto.getStartDatetime(),
                            formatter
                    );

            LocalDateTime endDateTime =
                    LocalDateTime.parse(
                            calendarEventDto.getEndDatetime(),
                            formatter
                    );

            start =
                    Map.of(
                            "dateTime",
                            startDateTime
                                    .atZone(KOREA_TIME_ZONE)
                                    .format(
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                    ),

                            "timeZone",
                            "Asia/Seoul"
                    );

            end =
                    Map.of(
                            "dateTime",
                            endDateTime
                                    .atZone(KOREA_TIME_ZONE)
                                    .format(
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                    ),

                            "timeZone",
                            "Asia/Seoul"
                    );

        } else {

            start =
                    Map.of(
                            "date",
                            eventDate
                    );

            end =
                    Map.of(
                            "date",
                            LocalDate.parse(eventDate)
                                    .plusDays(1)
                                    .toString()
                    );
        }

        Map<String, Object> googleEvent =
                Map.of(
                        "summary",
                        title,

                        "description",
                        content == null ? "" : content,

                        "location",
                        location == null ? "" : location,

                        "start",
                        start,

                        "end",
                        end
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        googleEvent,
                        headers
                );

        RestTemplate restTemplate =
                new RestTemplate();

        ResponseEntity<Map> response =
                restTemplate.postForEntity(
                        eventsInsertUrl,
                        request,
                        Map.class
                );

        Map<String, Object> body =
                response.getBody();

        if (body == null) {
            return null;
        }

        return (String) body.get("id");
    }

    // =========================
// 우리 사이트 → 구글 일정 수정
// 역할: 사이트에서 변경한 내용을 구글 캘린더에도 반영
// =========================
    @Transactional
    public void updateGoogleEvent(
            Integer memberNo,
            CalendarEventDto calendarEventDto
    ) {

        GoogleCalendarTokenDto tokenDto =
                googleCalendarTokenDao.findByMemberNo(
                        memberNo
                );

        // 구글 캘린더 연동 정보가 없으면 수정할 수 없음
        if (
                tokenDto == null
                        || tokenDto.getRefreshToken() == null
        ) {
            throw new IllegalStateException(
                    "구글 캘린더 연동 정보가 없습니다."
            );
        }

        String googleEventId =
                calendarEventDto.getGoogleEventId();

        // 수정할 구글 일정 번호 확인
        if (
                googleEventId == null
                        || googleEventId.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "수정할 구글 일정 ID가 없습니다."
            );
        }

        String eventDate =
                calendarEventDto.getEventDate();

        if (
                eventDate == null
                        || eventDate.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "일정 날짜가 없습니다."
            );
        }

        String accessToken =
                getValidAccessToken(
                        memberNo
                );

        String title =
                calendarEventDto.getTitle();

        String content =
                calendarEventDto.getContent();

        String location =
                calendarEventDto.getLocation();

        if (
                title == null
                        || title.isBlank()
        ) {
            title = "제목 없음";
        }


        // 구글에 전달할 시작·종료 날짜 또는 시간
        Map<String, Object> start;
        Map<String, Object> end;


    /*
        시간이 있는 일정은 dateTime으로 전송하고,
        시간이 없는 일정은 날짜 일정으로 전송한다.
    */
        if (
                "N".equals(
                        calendarEventDto.getAllDayYn()
                )
        ) {

            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern(
                            "yyyy-MM-dd HH:mm"
                    );

            LocalDateTime startDateTime =
                    LocalDateTime.parse(
                            calendarEventDto.getStartDatetime(),
                            formatter
                    );

            LocalDateTime endDateTime =
                    LocalDateTime.parse(
                            calendarEventDto.getEndDatetime(),
                            formatter
                    );

            start =
                    Map.of(
                            "dateTime",
                            startDateTime
                                    .atZone(KOREA_TIME_ZONE)
                                    .format(
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                    ),

                            "timeZone",
                            "Asia/Seoul"
                    );

            end =
                    Map.of(
                            "dateTime",
                            endDateTime
                                    .atZone(KOREA_TIME_ZONE)
                                    .format(
                                            DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                    ),

                            "timeZone",
                            "Asia/Seoul"
                    );

        } else {

            // 시간이 없는 일정은 구글의 종일 일정 형식으로 전송
            start =
                    Map.of(
                            "date",
                            eventDate
                    );

            end =
                    Map.of(
                            "date",
                            LocalDate.parse(
                                    eventDate
                            ).plusDays(1).toString()
                    );
        }


        // 구글 캘린더로 전송할 수정 내용
        Map<String, Object> googleEvent =
                Map.of(
                        "summary",
                        title,

                        "description",
                        content == null
                                ? ""
                                : content,

                        "location",
                        location == null
                                ? ""
                                : location,

                        "start",
                        start,

                        "end",
                        end
                );


        String updateUrl =
                "https://www.googleapis.com/calendar/v3/calendars/primary/events/"
                        + googleEventId;

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(
                accessToken
        );

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        googleEvent,
                        headers
                );

        RestTemplate restTemplate =
                new RestTemplate();


        // 기존 구글 일정을 새 내용으로 수정
        restTemplate.exchange(
                updateUrl,
                HttpMethod.PUT,
                request,
                Map.class
        );
    }

    // =========================
    // 우리 사이트 → 구글 일정 삭제
    // =========================
    @Transactional
    public void deleteGoogleEvent(
            Integer memberNo,
            String googleEventId
    ) {

        GoogleCalendarTokenDto tokenDto =
                googleCalendarTokenDao.findByMemberNo(memberNo);

        if (tokenDto == null
                || tokenDto.getRefreshToken() == null) {

            return;
        }

        if (googleEventId == null
                || googleEventId.isBlank()) {

            return;
        }

        String accessToken =
                getValidAccessToken(memberNo);

        String deleteUrl =
                "https://www.googleapis.com/calendar/v3/calendars/primary/events/"
                        + googleEventId;

        HttpHeaders headers =
                new HttpHeaders();

        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        RestTemplate restTemplate =
                new RestTemplate();

        try {
            restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    request,
                    Void.class
            );

        } catch (HttpClientErrorException.NotFound e) {

            // 이미 구글에서 삭제된 일정이면 우리 사이트 삭제만 진행
        }
    }


    // =========================
    // 구글 캘린더 연동 해제
    // =========================
    @Transactional
    public void disconnectGoogleCalendar(Integer memberNo) {

        googleCalendarTokenDao.deleteByMemberNo(memberNo);
    }


    // =========================
    // 대한민국 휴일 조회 URL 생성
    // 역할: 공개 캘린더 ID와 API Key를 이용해 조회 URL 생성
    // =========================
    private URI createKoreaHolidayEventsUri(
            String timeMin,
            String timeMax,
            String pageToken
    ) {

        UriComponentsBuilder builder =
                UriComponentsBuilder
                        .fromUriString(
                                "https://www.googleapis.com/calendar/v3"
                        )
                        .pathSegment("calendars")
                        .pathSegment(
                                KOREA_HOLIDAY_CALENDAR_ID
                        )
                        .pathSegment("events")
                        .queryParam(
                                "singleEvents",
                                "true"
                        )
                        .queryParam(
                                "orderBy",
                                "startTime"
                        )
                        .queryParam(
                                "maxResults",
                                "250"
                        )
                        .queryParam(
                                "timeMin",
                                timeMin
                        )
                        .queryParam(
                                "timeMax",
                                timeMax
                        )
                        .queryParam(
                                "timeZone",
                                "Asia/Seoul"
                        )
                        .queryParam(
                                "key",
                                holidayApiKey
                        );

        if (pageToken != null
                && !pageToken.isBlank()) {

            builder.queryParam(
                    "pageToken",
                    pageToken
            );
        }

        /*
            pathSegment와 encode를 사용하면
            캘린더 ID의 #이 %23으로 변환된다.
        */
        return builder
                .build()
                .encode()
                .toUri();
    }


    // =========================
    // 구글 날짜 변환
    // =========================
    private String convertGoogleDateTime(
            Map<String, Object> dateMap,
            boolean isStart
    ) {

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"
                );

        if (dateMap.containsKey("dateTime")) {

            String dateTime =
                    (String) dateMap.get("dateTime");

            return OffsetDateTime.parse(dateTime)
                    .toLocalDateTime()
                    .format(formatter);
        }

        String date =
                (String) dateMap.get("date");

        if (isStart) {
            return date + " 00:00";
        }

        return LocalDate.parse(date)
                .minusDays(1)
                .toString()
                + " 23:59";
    }


    // =========================
    // 구글 수정일 변환
    // =========================
    private String convertGoogleUpdatedDatetime(
            String updated
    ) {

        if (updated == null || updated.isBlank()) {
            return null;
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "yyyy-MM-dd HH:mm"
                );

        return OffsetDateTime.parse(updated)
                .toLocalDateTime()
                .format(formatter);
    }


    // =========================
    // URL 인코딩
    // =========================
    private String encode(String value) {

        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }
}