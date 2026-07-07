package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatRedisService chatRedisService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    private final Map<Integer, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionRoomMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 연결 성공 sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);

        String type = root.has("type") ? root.get("type").asText() : "MESSAGE";

        if ("JOIN".equals(type)) {
            Integer roomNo = root.get("roomNo").asInt();
            Integer viewerNo = root.get("viewerNo").asInt();

            joinRoom(roomNo, session);
            updateReadAndBroadcast(roomNo, viewerNo);
            return;
        }

        if ("READ".equals(type)) {
            Integer roomNo = root.get("roomNo").asInt();
            Integer viewerNo = root.get("viewerNo").asInt();

            updateReadAndBroadcast(roomNo, viewerNo);
            return;
        }

        if ("MESSAGE".equals(type)) {
            ChatMessageDto chatMessageDto;

            if (root.has("message")) {
                chatMessageDto = objectMapper.treeToValue(root.get("message"), ChatMessageDto.class);
            } else {
                chatMessageDto = objectMapper.treeToValue(root, ChatMessageDto.class);
            }

            chatMessageDto.setCreatedDate(LocalDateTime.now());

            if (chatMessageDto.getReadYn() == null) {
                chatMessageDto.setReadYn("N");
            }

            Integer roomNo = chatMessageDto.getRoomNo();

            joinRoom(roomNo, session);

            chatRedisService.saveMessage(chatMessageDto);

            broadcastToRoom(roomNo, Map.of(
                    "type", "MESSAGE",
                    "message", chatMessageDto
            ));
        }
    }

    private void joinRoom(Integer roomNo, WebSocketSession session) {
        roomSessionMap
                .computeIfAbsent(roomNo, key -> ConcurrentHashMap.newKeySet())
                .add(session);

        sessionRoomMap.put(session.getId(), roomNo);
    }

    private void updateReadAndBroadcast(Integer roomNo, Integer viewerNo) {
        chatService.updateReadYn(roomNo, viewerNo);
        chatRedisService.updateReadYn(roomNo, viewerNo);

        broadcastToRoom(roomNo, Map.of(
                "type", "READ",
                "roomNo", roomNo,
                "viewerNo", viewerNo
        ));
    }

    public void broadcastClose(Integer roomNo, ChatMessageDto closeMessage) {
        broadcastToRoom(roomNo, Map.of(
                "type", "CLOSE",
                "roomNo", roomNo,
                "message", closeMessage
        ));
    }

    private void broadcastToRoom(Integer roomNo, Object data) {
        Set<WebSocketSession> sessions = roomSessionMap.get(roomNo);

        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(data);

            for (WebSocketSession wsSession : sessions) {
                if (wsSession.isOpen()) {
                    wsSession.sendMessage(new TextMessage(json));
                }
            }
        } catch (Exception e) {
            log.error("WebSocket 메시지 전송 실패 roomNo={}", roomNo, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결 종료 sessionId={}", session.getId());

        Integer roomNo = sessionRoomMap.remove(session.getId());

        if (roomNo != null) {
            Set<WebSocketSession> sessions = roomSessionMap.get(roomNo);

            if (sessions != null) {
                sessions.remove(session);

                if (sessions.isEmpty()) {
                    roomSessionMap.remove(roomNo);
                }
            }
        }
    }
}