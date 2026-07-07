package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.lettuce.core.pubsub.PubSubOutput.Type.message;

@Service
@RequiredArgsConstructor
public class ChatRedisService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CHAT_KEY_PREFIX = "chat:room";

    public void saveMessage(ChatMessageDto messageDto) {
        try {
            if (messageDto.getCreatedDate() == null) {
                messageDto.setCreatedDate(LocalDateTime.now());
            }

            if (messageDto.getReadYn() == null) {
                messageDto.setReadYn("N");
            }

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
                ChatMessageDto messageDto =
                        objectMapper.readValue(json, ChatMessageDto.class);
                messages.add(messageDto);
            } catch (Exception e){
                throw new RuntimeException("Redis 메시지 조회 실패", e);
            }
        }
        return messages;
    }
    public void deleteMessages(Integer roomNo){
        String key = CHAT_KEY_PREFIX + roomNo;
        redisTemplate.delete(key);
    }

    //메시지 수신 상태 표시
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        String key = CHAT_KEY_PREFIX + roomNo;

        List<String> jsonList = redisTemplate.opsForList().range(key, 0, -1);

        if (jsonList == null || jsonList.isEmpty()) {
            return;
        }
        redisTemplate.delete(key);

        for (String json : jsonList) {
            try {
                ChatMessageDto messageDto =
                        objectMapper.readValue(json, ChatMessageDto.class);
                if (messageDto.getSenderNo() != null
                        && !messageDto.getSenderNo().equals(viewerNo)) {
                    messageDto.setReadYn("Y");
                }
                String updateJson = objectMapper.writeValueAsString(messageDto);
                redisTemplate.opsForList().rightPush(key, updateJson);

            } catch (Exception e) {
                throw new RuntimeException("Redis 읽음 처리 실패", e);
            }
        }
    }

    public int countUnreadMessages(Integer roomNo, Integer viewerNo) {
        List<ChatMessageDto> messages = findMessages(roomNo);

        int count = 0;

        for (ChatMessageDto message : messages) {
            if (message.getSenderNo() == null) {
                continue;
            }

            if (message.getSenderNo().equals(viewerNo)) {
                continue;
            }

            if ("N".equals(message.getReadYn())) {
                count++;
            }
        }

        return count;
    }
}

