package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis에 채팅 메시지를 임시 저장하고 조회/삭제/읽음 처리하는 Service
 *
 * 메시지는 WebSocket 수신 시 Redis에 먼저 저장되고,
 * Scheduler가 일정 주기마다 Oracle DB로 옮긴다.
 */
@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Redis key 예시: chat:room64
     */
    private static final String CHAT_KEY_PREFIX = "chat:room";

    /**
     * 메시지를 Redis List에 저장한다.
     */
    public void saveMessage(ChatMessageDto messageDto) {
        try {
            if (messageDto == null || messageDto.getRoomNo() == null) {
                return;
            }

            if (messageDto.getCreatedDate() == null) {
                messageDto.setCreatedDate(LocalDateTime.now());
            }

            if (!"Y".equals(messageDto.getReadYn()) && !"N".equals(messageDto.getReadYn())) {
                messageDto.setReadYn("N");
            }

            String key = createKey(messageDto.getRoomNo());
            String json = objectMapper.writeValueAsString(messageDto);

            redisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e) {
            throw new RuntimeException("Redis 메시지 저장 실패", e);
        }
    }

    /**
     * Redis에 저장된 특정 상담방 메시지 목록 조회
     */
    public List<ChatMessageDto> findMessages(Integer roomNo) {
        String key = createKey(roomNo);

        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);
        List<ChatMessageDto> messages = new ArrayList<>();

        if (jsonList == null) {
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

    /**
     * 특정 상담방 Redis 메시지 전체 삭제
     *
     * 관리자 하드 DELETE 시 사용한다.
     */
    public void deleteMessages(Integer roomNo) {
        String key = createKey(roomNo);
        redisTemplate.delete(key);
    }

    /**
     * Scheduler가 Oracle에 저장 완료한 메시지만 Redis에서 제거한다.
     *
     * 기존 deleteMessages(roomNo)를 사용하면 Scheduler 저장 중 새로 들어온 메시지까지
     * 같이 삭제될 수 있으므로, 저장 완료한 개수만큼 앞에서 제거한다.
     */
    public void deleteSavedMessages(Integer roomNo, int savedCount) {
        if (roomNo == null || savedCount <= 0) {
            return;
        }

        String key = createKey(roomNo);
        Long currentSize = redisTemplate.opsForList().size(key);

        if (currentSize == null || currentSize <= savedCount) {
            redisTemplate.delete(key);
            return;
        }

        redisTemplate.opsForList().trim(key, savedCount, -1);
    }

    /**
     * Redis에 남아 있는 메시지 읽음 처리
     *
     * 현재 접속자가 보낸 메시지는 제외하고,
     * 상대방이 보낸 메시지만 readYn = Y 로 변경한다.
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

        redisTemplate.delete(key);

        for (String json : jsonList) {
            try {
                ChatMessageDto messageDto = objectMapper.readValue(json, ChatMessageDto.class);

                if (messageDto.getSenderNo() != null && !messageDto.getSenderNo().equals(viewerNo)) {
                    messageDto.setReadYn("Y");
                }

                String updateJson = objectMapper.writeValueAsString(messageDto);
                redisTemplate.opsForList().rightPush(key, updateJson);
            } catch (Exception e) {
                throw new RuntimeException("Redis 읽음 처리 실패", e);
            }
        }
    }

    /**
     * Redis에 아직 남아 있는 안읽음 메시지 개수 계산
     *
     * 관리자 목록에서는 DB 안읽음 개수와 Redis 안읽음 개수를 합쳐서 표시한다.
     */
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

            if ("N".equals(message.getReadYn())) {
                count++;
            }
        }

        return count;
    }

    private String createKey(Integer roomNo) {
        return CHAT_KEY_PREFIX + roomNo;
    }
}