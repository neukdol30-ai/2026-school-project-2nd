package com.siyan1234.itproject2nd.member.service;

import com.siyan1234.itproject2nd.member.dto.PendingSocialSignupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// 약관 동의 전 소셜 가입 정보 Redis 임시 저장 -> 조회, 삭제 Service
@Service
@RequiredArgsConstructor
public class PendingSocialSignupService {

    // Redis 소셜 가입 대기 정보 구분 키
    private static final String KEY_PREFIX = "social:pending:";

    // 약관 동의 화면 방치 시 임시 정보 유지 시간 -> 10분 지나면 Redis가 키와 개인정보 자동 삭제
    private static final Duration PENDING_TTL = Duration.ofMinutes(10);

    // Redis Hash 내부에서 사용할 필드 이름들
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_PROVIDER_ID = "providerId";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_NICKNAME = "nickname";
    private static final String FIELD_EMAIL = "email";

    // Redis에 문자열 자료를 저장, 조회, 삭제하는 도구
    private final StringRedisTemplate redisTemplate;

    public String save(PendingSocialSignupDto pendingSignup) {

        if (pendingSignup == null
                || pendingSignup.getProvider() == null
                || pendingSignup.getProvider().isBlank()
                || pendingSignup.getProviderId() == null
                || pendingSignup.getProviderId().isBlank()) {

            throw new IllegalArgumentException(
                    "소셜 가입 대기정보의 provider 또는 providerId가 없습니다."
            );
        }

        String pendingToken = UUID.randomUUID().toString();

        String redisKey = buildRedisKey(pendingToken);

        Map<String, String> values = new HashMap<>();

        values.put(
                FIELD_PROVIDER,
                nullToEmpty(pendingSignup.getProvider())
        );

        values.put(
                FIELD_PROVIDER_ID,
                nullToEmpty(pendingSignup.getProviderId())
        );

        values.put(
                FIELD_NAME,
                nullToEmpty(pendingSignup.getName())
        );

        values.put(
                FIELD_NICKNAME,
                nullToEmpty(pendingSignup.getNickname())
        );

        values.put(
                FIELD_EMAIL,
                nullToEmpty(pendingSignup.getEmail())
        );

        redisTemplate.opsForHash().putAll(redisKey, values);

        redisTemplate.expire(redisKey, PENDING_TTL);

        return pendingToken;
    }

    // Redis에 저장된 소셜 가입 대기 정보 조회
    public PendingSocialSignupDto find(String pendingToken) {

        if (pendingToken == null || pendingToken.isBlank()) {
            return null;
        }

        String redisKey = buildRedisKey(pendingToken);

        Map<Object, Object> values =
                redisTemplate.opsForHash().entries(redisKey);

        if (values == null || values.isEmpty()) {
            return null;
        }

        return PendingSocialSignupDto.builder()
                .provider(readValue(values, FIELD_PROVIDER))
                .providerId(readValue(values, FIELD_PROVIDER_ID))
                .name(readValue(values, FIELD_NAME))
                .nickname(readValue(values, FIELD_NICKNAME))
                .email(readValue(values, FIELD_EMAIL))
                .build();
    }

    // 약관 동의 완료 또는 가입 취소 시 Redis 임시 정보 즉시 삭제
    public void delete(String pendingToken) {

        // 빈 토큰 Redis 키 차단
        if (pendingToken == null || pendingToken.isBlank()) {
            return;
        }

        // 만료 시간 기다리지 않고 즉시 임시 가입 정보 삭제
        redisTemplate.delete(buildRedisKey(pendingToken));
    }

    // 임의 토큰을 실제 Redis 키로 변환
    private String buildRedisKey(String pendingToken) {

        return KEY_PREFIX + pendingToken;
    }

    private String readValue(
            Map<Object, Object> values,
            String fieldName
    ) {

        Object rawValue = values.get(fieldName);

        if (rawValue == null) {
            return null;
        }

        String value = rawValue.toString();

        return value.isBlank() ? null : value;
    }

    private String nullToEmpty(String value) {

        return value == null ? "" : value;
    }
}
