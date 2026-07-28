package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * WebSocket JSON payload 파싱 전용 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class ChatWebSocketPayloadParser {

    private final ObjectMapper objectMapper;

    public JsonNode readTree(String payload) throws Exception {
        return objectMapper.readTree(payload);
    }

    public String getType(JsonNode root) {
        if (root == null || !root.has("type")) {
            return ChatWebSocketEventType.MESSAGE;
        }

        return root.get("type").asText();
    }

    public Integer getInteger(JsonNode root, String fieldName) {
        if (root == null || !root.has(fieldName) || root.get(fieldName) == null || root.get(fieldName).isNull()) {
            return null;
        }

        return root.get(fieldName).asInt();
    }

    public ChatMessageDto parseChatMessage(JsonNode root) throws Exception {
        if (root.has("message")) {
            return objectMapper.treeToValue(root.get("message"), ChatMessageDto.class);
        }

        return objectMapper.treeToValue(root, ChatMessageDto.class);
    }
}
