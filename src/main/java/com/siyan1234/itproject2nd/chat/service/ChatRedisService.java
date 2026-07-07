package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;


import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY_PREFIX = "chat:room";

    public void saveMessage(ChatMessageDto messageDto) {
        try {
            String key = CHAT_KEY_PREFIX + messageDto.getRoomNo();
            String json = objectMapper.writeValueAsString(messageDto);

            redisTemplate.opsForList().rightPush(key, json);
        } catch (Exception e){
            throw new RuntimeException("Redis 메시지 저장 실패", e);
        }
    }
    public List<ChatMessageDto> findMessages(Integer roomNo){
        String key = CHAT_KEY_PREFIX + roomNo;

        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

        List<ChatMessageDto> messages = new ArrayList<>();

        if (jsonList == null){
            return messages;
        }
        for (String json : jsonList){
            try {
                messages.add(objectMapper.readValue(json, ChatMessageDto.class));
            } catch (Exception e){
                throw new RuntimeException("Redis 메시지 조회 실패", e);
            }
        }
        return messages;
    }
    public List<ChatMessageDto> popAllMessages(Integer roomNo){
        String key = CHAT_KEY_PREFIX + roomNo;
        List<ChatMessageDto> messages = findMessages(roomNo);
        redisTemplate.delete(key);
        return messages;
    }
}

