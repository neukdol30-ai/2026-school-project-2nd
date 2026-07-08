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

    // 채팅방별 접속자 목록
    private final Map<Integer, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();

    // 세션이 어느 채팅방에 들어가 있는지 저장
    private final Map<String, Integer> sessionRoomMap = new ConcurrentHashMap<>();

    // 관리자 상담 목록 페이지 접속자 목록
    private final Set<WebSocketSession> adminListSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 연결 성공 sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);

        String type = root.has("type") ? root.get("type").asText() : "MESSAGE";

        // 관리자 상담 목록 페이지 접속
        if ("ADMIN_LIST_JOIN".equals(type)) {
            adminListSessions.add(session);
            log.info("관리자 상담 목록 WebSocket 등록 sessionId={}", session.getId());
            return;
        }

        // 채팅방 접속
        if ("JOIN".equals(type)) {
            Integer roomNo = root.get("roomNo").asInt();
            Integer viewerNo = root.get("viewerNo").asInt();

            joinRoom(roomNo, session);
            updateReadAndBroadcast(roomNo, viewerNo);
            broadcastAdminListRefresh(roomNo);
            return;
        }

        // 읽음 처리
        if ("READ".equals(type)) {
            Integer roomNo = root.get("roomNo").asInt();
            Integer viewerNo = root.get("viewerNo").asInt();

            updateReadAndBroadcast(roomNo, viewerNo);
            broadcastAdminListRefresh(roomNo);
            return;
        }

        // 메시지 전송
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

            // 메시지는 Redis에 먼저 저장
            chatRedisService.saveMessage(chatMessageDto);

            // 관리자 목록 정렬/마지막 메시지 표시용으로 chat_room은 즉시 갱신
            chatService.updateLastMessage(roomNo, chatMessageDto.getMessageContent());

            // 채팅방 내부 실시간 전송
            broadcastToRoom(roomNo, Map.of(
                    "type", "MESSAGE",
                    "message", chatMessageDto
            ));

            // 관리자 상담 목록 실시간 갱신 알림
            broadcastAdminListRefresh(roomNo);
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

    public void broadcastAdminListRefresh(Integer roomNo) {
        broadcastToAdminList(Map.of(
                "type", "ADMIN_ROOM_REFRESH",
                "roomNo", roomNo
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
            log.error("WebSocket 채팅방 메시지 전송 실패 roomNo={}", roomNo, e);
        }
    }

    private void broadcastToAdminList(Object data) {
        if (adminListSessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(data);

            for (WebSocketSession wsSession : adminListSessions) {
                if (wsSession.isOpen()) {
                    wsSession.sendMessage(new TextMessage(json));
                } else {
                    adminListSessions.remove(wsSession);
                }
            }
        } catch (Exception e) {
            log.error("관리자 상담 목록 WebSocket 전송 실패", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결 종료 sessionId={}", session.getId());

        adminListSessions.remove(session);

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