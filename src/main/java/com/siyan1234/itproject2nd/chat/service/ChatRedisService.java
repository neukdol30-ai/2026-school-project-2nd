package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.support.ChatReadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis 채팅 메시지 임시 저장 Service
 *
 * 메시지 처리 흐름:
 * 1. WebSocket 수신 메시지를 Redis List에 먼저 저장한다.
 * 2. Scheduler가 Redis 메시지를 Oracle DB로 옮긴다.
 * 3. DB 저장이 완료된 메시지 개수만큼만 Redis 앞쪽에서 제거한다.
 */
@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** Redis key 예시: chat:room64 */
    private static final String CHAT_KEY_PREFIX = "chat:room";

    /** 메시지를 Redis List 뒤쪽에 저장한다. */
    public void saveMessage(ChatMessageDto messageDto) {
        try {
            if (messageDto == null || messageDto.getRoomNo() == null) {
                return;
            }

            if (messageDto.getCreatedDate() == null) {
                messageDto.setCreatedDate(LocalDateTime.now());
            }

            messageDto.setReadYn(ChatReadStatus.normalize(messageDto.getReadYn()));

            String key = createKey(messageDto.getRoomNo());
            String json = objectMapper.writeValueAsString(messageDto);

            redisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e) {
            throw new RuntimeException("Redis 메시지 저장 실패", e);
        }
    }

    /** Redis에 남아 있는 특정 상담방 메시지 목록 조회 */
    public List<ChatMessageDto> findMessages(Integer roomNo) {
        if (roomNo == null) {
            return List.of();
        }

        String key = createKey(roomNo);
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        List<ChatMessageDto> messages = new ArrayList<>();

        if (jsonList == null || jsonList.isEmpty()) {
            return messages;
        }

        for (String json : jsonList) {
            try {
                ChatMessageDto messageDto = objectMapper.readValue(json, ChatMessageDto.class);
                messages.add(messageDto);
            } catch (Exception e) {
                throw new RuntimeException("Redis 메시지 조회 실패", e);
            }
        }

        return messages;
    }

    /** 상담방 삭제 시 Redis 메시지 전체 삭제 */
    public void deleteMessages(Integer roomNo) {
        if (roomNo == null) {
            return;
        }

        String key = createKey(roomNo);
        redisTemplate.delete(key);
    }

    /**
     * Scheduler가 Oracle에 저장 완료한 메시지 개수만큼 Redis 앞쪽에서 제거한다.
     *
     * 예:
     * - Scheduler가 5개를 읽어서 DB에 저장하는 동안 새 메시지 2개가 Redis 뒤에 추가됨
     * - 전체 delete가 아니라 앞의 5개만 제거해야 새 메시지 2개가 소실되지 않음
     */
    public void deleteSavedMessages(Integer roomNo, int savedCount) {
        if (roomNo == null || savedCount <= 0) {
            return;
        }

        String key = createKey(roomNo);
        Long currentSize = redisTemplate.opsForList().size(key);

        if (currentSize == null || currentSize == 0) {
            return;
        }

        if (currentSize <= savedCount) {
            redisTemplate.delete(key);
            return;
        }

        redisTemplate.opsForList().trim(key, savedCount, -1);
    }

    /**
     * Redis 메시지 읽음 처리
     *
     * 주의:
     * - Redis key를 delete 후 재삽입하지 않는다.
     * - 기존 List index 위치의 값만 set으로 바꿔 메시지 순서와 데이터 소실 위험을 줄인다.
     */
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        if (roomNo == null || viewerNo == null) {
            return;
        }

        String key = createKey(roomNo);
        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            return;
        }

        for (int i = 0; i < jsonList.size(); i++) {
            String json = jsonList.get(i);

            try {
                ChatMessageDto messageDto = objectMapper.readValue(json, ChatMessageDto.class);

                if (messageDto.getSenderNo() == null) {
                    continue;
                }

                if (messageDto.getSenderNo().equals(viewerNo)) {
                    continue;
                }

                if (!ChatReadStatus.isUnread(messageDto.getReadYn())) {
                    continue;
                }

                messageDto.setReadYn(ChatReadStatus.READ);
                String updateJson = objectMapper.writeValueAsString(messageDto);

                redisTemplate.opsForList().set(key, i, updateJson);
            } catch (Exception e) {
                throw new RuntimeException("Redis 읽음 처리 실패", e);
            }
        }
    }

    /** Redis에 남아 있는 안읽음 메시지 개수 계산 */
    public int countUnreadMessages(Integer roomNo, Integer viewerNo) {
        List<ChatMessageDto> messages = findMessages(roomNo);
        int count = 0;

        for (ChatMessageDto message : messages) {
            if (message.getSenderNo() == null) {
                continue;
            }

            if (viewerNo != null && message.getSenderNo().equals(viewerNo)) {
                continue;
            }

            if (ChatReadStatus.isUnread(message.getReadYn())) {
                count++;
            }
        }

        return count;
    }

    private String createKey(Integer roomNo) {
        return CHAT_KEY_PREFIX + roomNo;
    }
}
