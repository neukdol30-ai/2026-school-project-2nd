package com.siyan1234.itproject2nd.map.controller;

import com.siyan1234.itproject2nd.map.support.KakaoMapApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 카카오맵 화면 컨트롤러입니다.
 *
 * 4차 패치부터 실제 사용자 화면은 Kakao Map JavaScript SDK 기반 동적지도로 전환합니다.
 */
@Controller
@RequiredArgsConstructor
public class KakaoMapPageController {

    private final KakaoMapApiProperties properties;

    @GetMapping("/map/kakao")
    public String kakaoMapPage(Model model) {
        model.addAttribute("kakaoMapReady", properties.hasJavascriptKey());
        model.addAttribute("kakaoMapSdkUrl", buildKakaoMapSdkUrl());
        return "map/kakao-map";
    }

    private String buildKakaoMapSdkUrl() {
        if (!properties.hasJavascriptKey()) {
            return "";
        }

        return "https://dapi.kakao.com/v2/maps/sdk.js?appkey="
                + properties.getJavascriptKey()
                + "&libraries=services";
    }
}
