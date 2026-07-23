package com.siyan1234.itproject2nd.map.controller;

import com.siyan1234.itproject2nd.map.dto.KakaoMapActionResponseDto;
import com.siyan1234.itproject2nd.map.service.KakaoMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
}
