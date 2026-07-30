package com.siyan1234.itproject2nd.board.service;

import com.siyan1234.itproject2nd.board.exception.BoardRateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class BoardWriteRateLimitService {

    private final StringRedisTemplate redisTemplate;

    /*
     * 같은 회원이 글을 다시 작성할 때 기다려야 하는 시간
     */
    private static final int COOLDOWN_SECONDS = 10;

    /*
     * 1분 동안 허용할 최대 작성 수
     */
    private static final int MINUTE_LIMIT = 5;

    /*
     * 하루 동안 허용할 최대 작성 수
     */
    private static final int DAILY_LIMIT = 30;

    /*
     * Redis INCR와 EXPIRE를 한 번에 실행하는 Lua 스크립트
     */
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local count = redis.call('INCR', KEYS[1])

                    if count == 1 then
                        redis.call('EXPIRE', KEYS[1], ARGV[1])
                    end

                    return count
                    """,
                    Long.class
            );

    public void validateWrite(Long memberNo) {

        if (memberNo == null) {
            throw new BoardRateLimitException(
                    "로그인 정보를 확인할 수 없습니다."
            );
        }

        /*
         * 1. 연속 등록 제한
         */
        checkCooldown(memberNo);

        /*
         * 2. 1분 작성 횟수 제한
         */
        checkMinuteLimit(memberNo);

        /*
         * 3. 하루 작성 횟수 제한
         */
        checkDailyLimit(memberNo);
    }

    private void checkCooldown(Long memberNo) {

        String key =
                "board:write:cooldown:member:" + memberNo;

        Boolean created =
                redisTemplate
                        .opsForValue()
                        .setIfAbsent(
                                key,
                                "1",
                                Duration.ofSeconds(
                                        COOLDOWN_SECONDS
                                )
                        );

        if (Boolean.FALSE.equals(created)) {
            throw new BoardRateLimitException(
                    "게시글은 " + COOLDOWN_SECONDS
                            + "초에 한 번만 작성할 수 있습니다."
            );
        }
    }

    private void checkMinuteLimit(Long memberNo) {

        String minute =
                LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern(
                                "yyyyMMddHHmm"
                        )
                );

        String key =
                "board:write:minute:"
                        + memberNo
                        + ":"
                        + minute;

        Long count =
                incrementWithExpire(
                        key,
                        120
                );

        if (count != null && count > MINUTE_LIMIT) {
            throw new BoardRateLimitException(
                    "게시글을 너무 빠르게 작성하고 있습니다. "
                            + "잠시 후 다시 시도해주세요."
            );
        }
    }

    private void checkDailyLimit(Long memberNo) {

        String today =
                LocalDate.now().format(
                        DateTimeFormatter.BASIC_ISO_DATE
                );

        String key =
                "board:write:daily:"
                        + memberNo
                        + ":"
                        + today;

        /*
         * 26시간 뒤 자동 삭제
         */
        Long count =
                incrementWithExpire(
                        key,
                        26 * 60 * 60
                );

        if (count != null && count > DAILY_LIMIT) {
            throw new BoardRateLimitException(
                    "하루에 작성할 수 있는 게시글 수를 초과했습니다."
            );
        }
    }

    private Long incrementWithExpire(
            String key,
            long expireSeconds
    ) {
        return redisTemplate.execute(
                INCREMENT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(expireSeconds)
        );
    }
}
