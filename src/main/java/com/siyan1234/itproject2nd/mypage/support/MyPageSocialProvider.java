package com.siyan1234.itproject2nd.mypage.support;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 마이페이지에서 사용하는 소셜 로그인 제공자 표시 정책입니다.
 *
 * DB에는 social_account.provider 값이 영문 코드(KAKAO, NAVER)로 저장되므로,
 * 화면 응답에서는 이 클래스를 통해 한글 표시명과 연결 여부를 일관되게 변환합니다.
 */
public final class MyPageSocialProvider {

    public static final String KAKAO = "KAKAO";
    public static final String NAVER = "NAVER";

    private MyPageSocialProvider() {
    }

    /**
     * LISTAGG로 합쳐진 provider 문자열을 대문자 provider 목록으로 변환합니다.
     * 예: "KAKAO, NAVER" -> ["KAKAO", "NAVER"]
     */
    public static List<String> normalizeProviders(String providers) {
        if (providers == null || providers.isBlank()) {
            return List.of();
        }

        return Arrays.stream(providers.split(","))
                .map(String::trim)
                .filter(provider -> !provider.isBlank())
                .map(provider -> provider.toUpperCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    /** 특정 소셜 제공자가 연결되어 있는지 확인합니다. */
    public static boolean hasProvider(String providers, String targetProvider) {
        if (targetProvider == null || targetProvider.isBlank()) {
            return false;
        }

        String normalizedTarget = targetProvider.trim().toUpperCase(Locale.ROOT);
        return normalizeProviders(providers).contains(normalizedTarget);
    }

    /** 화면에 표시할 한글 소셜 제공자명입니다. */
    public static String toKoreanLabel(String provider) {
        if (KAKAO.equals(provider)) {
            return "카카오";
        }

        if (NAVER.equals(provider)) {
            return "네이버";
        }

        return provider;
    }

    /** 로그인 방식 안내 문구입니다. */
    public static String toLoginMethodLabel(String providers) {
        List<String> providerList = normalizeProviders(providers);
        if (providerList.isEmpty()) {
            return "소셜 로그인";
        }

        return providerList.stream()
                .map(MyPageSocialProvider::toKoreanLabel)
                .collect(Collectors.joining(", ")) + " 계정";
    }
}
