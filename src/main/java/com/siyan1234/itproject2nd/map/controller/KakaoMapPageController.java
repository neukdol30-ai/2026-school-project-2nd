package com.siyan1234.itproject2nd.map.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 카카오맵 기능 테스트 화면 컨트롤러입니다.
 */
@Controller
public class KakaoMapPageController {

    @GetMapping("/map/kakao")
    public String kakaoMapPage() {
        return "map/kakao-map";
    }
}
