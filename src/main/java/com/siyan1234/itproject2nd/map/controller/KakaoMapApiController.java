package com.siyan1234.itproject2nd.map.controller;

import com.siyan1234.itproject2nd.map.dto.KakaoMapActionResponseDto;
import com.siyan1234.itproject2nd.map.service.KakaoMapService;
import com.siyan1234.itproject2nd.map.support.KakaoMapApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 브라우저에서 호출하는 카카오맵 API 프록시 컨트롤러입니다.
 *
 * 카카오 REST API 키를 프론트에 노출하지 않기 위해 모든 요청은 이 컨트롤러를 거쳐 갑니다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kakao-map")
public class KakaoMapApiController {

    private final KakaoMapService kakaoMapService;

    @GetMapping("/address")
    public ResponseEntity<KakaoMapActionResponseDto> searchAddress(@RequestParam String query,
                                                                   @RequestParam(defaultValue = "1") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(kakaoMapService.searchAddress(query, page, size));
    }

    @GetMapping("/coord-to-address")
    public ResponseEntity<KakaoMapActionResponseDto> coordToAddress(@RequestParam String x,
                                                                    @RequestParam String y) {
        return ResponseEntity.ok(kakaoMapService.coordToAddress(x, y));
    }

    @GetMapping("/places")
    public ResponseEntity<KakaoMapActionResponseDto> searchPlaces(@RequestParam String query,
                                                                  @RequestParam(required = false) String x,
                                                                  @RequestParam(required = false) String y,
                                                                  @RequestParam(required = false) Integer radius,
                                                                  @RequestParam(defaultValue = "1") Integer page,
                                                                  @RequestParam(defaultValue = "10") Integer size,
                                                                  @RequestParam(defaultValue = "accuracy") String sort) {
        return ResponseEntity.ok(kakaoMapService.searchPlaces(query, x, y, radius, page, size, sort));
    }

    @GetMapping("/route")
    public ResponseEntity<KakaoMapActionResponseDto> findRoute(@RequestParam(defaultValue = "publictraffic") String type,
                                                               @RequestParam String startX,
                                                               @RequestParam String startY,
                                                               @RequestParam String endX,
                                                               @RequestParam String endY,
                                                               @RequestParam(required = false) String startName,
                                                               @RequestParam(required = false) String endName,
                                                               @RequestParam(defaultValue = "BROAD_FIRST") String routeMode) {
        return ResponseEntity.ok(kakaoMapService.findRoute(type, startX, startY, endX, endY, startName, endName, routeMode));
    }

    @GetMapping("/static")
    public ResponseEntity<byte[]> staticMap(@RequestParam String x,
                                            @RequestParam String y,
                                            @RequestParam(defaultValue = "640") Integer width,
                                            @RequestParam(defaultValue = "360") Integer height,
                                            @RequestParam(defaultValue = "3") Integer level,
                                            @RequestParam(defaultValue = "png") String format) {
        try {
            byte[] image = kakaoMapService.staticMapImage(x, y, width, height, level, format);
            return ResponseEntity.ok()
                    .contentType(resolveImageMediaType(format))
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(image);
        } catch (IllegalArgumentException e) {
            return textError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalStateException e) {
            return textError(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        } catch (KakaoMapApiException e) {
            return textError(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
    }

    private MediaType resolveImageMediaType(String format) {
        if ("jpg".equalsIgnoreCase(format)) {
            return MediaType.IMAGE_JPEG;
        }

        return MediaType.IMAGE_PNG;
    }

    private ResponseEntity<byte[]> textError(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_PLAIN)
                .body(String.valueOf(message).getBytes(StandardCharsets.UTF_8));
    }
}
