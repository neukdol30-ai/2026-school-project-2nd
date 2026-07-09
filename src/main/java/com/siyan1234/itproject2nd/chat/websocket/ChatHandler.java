package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
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

/**
 * WebSocket 실시간 채팅 처리 클래스
 *
 * 처리하는 이벤트:
 * - ADMIN_LIST_JOIN: 관리자 상담 목록 페이지 접속 등록
 * - JOIN: 특정 채팅방 접속
 * - READ: 메시지 읽음 처리
 * - MESSAGE: 실시간 메시지 전송
 *
 * 메시지는 Redis에 먼저 저장하고,
 * 관리자 목록 표시용 last_message는 Oracle chat_room에 즉시 반영한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatRedisService chatRedisService;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;

    /**
     * 채팅방 번호별 WebSocket 세션 목록
     */
    private final Map<Integer, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();

    /**
     * WebSocket 세션이 어느 채팅방에 들어가 있는지 저장
     */
    private final Map<String, Integer> sessionRoomMap = new ConcurrentHashMap<>();

    /**
     * 관리자 상담 목록 페이지에 접속 중인 WebSocket 세션 목록
     */
    private final Set<WebSocketSession> adminListSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 연결 성공 sessionId={}", session.getId());
    }

    /**
     * 클라이언트에서 보낸 WebSocket 메시지를 type 기준으로 분기 처리한다.
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);

        String type = root.has("type") ? root.get("type").asText() : "MESSAGE";

        if ("ADMIN_LIST_JOIN".equals(type)) {
            handleAdminListJoin(session);
            return;
        }

        if ("JOIN".equals(type)) {
            handleJoin(session, root);
            return;
        }

        if ("READ".equals(type)) {
            handleRead(root);
            return;
        }

        if ("MESSAGE".equals(type)) {
            handleMessage(session, root);
        }
    }

    /**
     * 관리자 상담 목록 페이지 접속 등록
     */
    private void handleAdminListJoin(WebSocketSession session) {
        adminListSessions.add(session);
        log.info("관리자 상담 목록 WebSocket 등록 sessionId={}", session.getId());
    }

    /**
     * 채팅방 입장 처리
     *
     * 사용자가 방에 들어오면 해당 세션을 roomSessionMap에 등록하고
     * 상대방 메시지를 읽음 처리한다.
     */
    private void handleJoin(WebSocketSession session, JsonNode root) {
        Integer roomNo = getInteger(root, "roomNo");
        Integer viewerNo = getInteger(root, "viewerNo");

        if (roomNo == null || viewerNo == null) {
            return;
        }

        joinRoom(roomNo, session);
        updateReadAndBroadcast(roomNo, viewerNo);
        broadcastAdminListRefresh(roomNo);
    }

    /**
     * 읽음 처리 이벤트
     */
    private void handleRead(JsonNode root) {
        Integer roomNo = getInteger(root, "roomNo");
        Integer viewerNo = getInteger(root, "viewerNo");

        if (roomNo == null || viewerNo == null) {
            return;
        }

        updateReadAndBroadcast(roomNo, viewerNo);
        broadcastAdminListRefresh(roomNo);
    }

    /**
     * 메시지 전송 이벤트
     *
     * 종료된 상담방에는 메시지를 저장하지 않는다.
     */
    private void handleMessage(WebSocketSession session, JsonNode root) throws Exception {
        ChatMessageDto chatMessageDto;

        if (root.has("message")) {
            chatMessageDto = objectMapper.treeToValue(root.get("message"), ChatMessageDto.class);
        } else {
            chatMessageDto = objectMapper.treeToValue(root, ChatMessageDto.class);
        }

        Integer roomNo = chatMessageDto.getRoomNo();

        if (roomNo == null) {
            return;
        }

        if (!isOpenRoom(roomNo)) {
            return;
        }

        chatMessageDto.setCreatedDate(LocalDateTime.now());

        if (!"Y".equals(chatMessageDto.getReadYn()) && !"N".equals(chatMessageDto.getReadYn())) {
            chatMessageDto.setReadYn("N");
        }

        joinRoom(roomNo, session);

        // 메시지는 Redis에 먼저 저장
        chatRedisService.saveMessage(chatMessageDto);

        // 관리자 목록 정렬과 마지막 메시지 표시를 위해 chat_room은 즉시 갱신
        chatService.updateLastMessage(roomNo, chatMessageDto.getMessageContent());

        // 채팅방 내부 사용자/관리자에게 메시지 전송
        broadcastToRoom(roomNo, Map.of(
                "type", "MESSAGE",
                "message", chatMessageDto
        ));

        // 관리자 상담 목록 실시간 갱신
        broadcastAdminListRefresh(roomNo);
    }

    /**
     * 세션을 특정 채팅방에 등록
     */
    private void joinRoom(Integer roomNo, WebSocketSession session) {
        roomSessionMap
                .computeIfAbsent(roomNo, key -> ConcurrentHashMap.newKeySet())
                .add(session);

        sessionRoomMap.put(session.getId(), roomNo);
    }

    /**
     * DB와 Redis의 메시지를 읽음 처리한 뒤 채팅방 내부에 READ 이벤트를 전송
     */
    private void updateReadAndBroadcast(Integer roomNo, Integer viewerNo) {
        chatService.updateReadYn(roomNo, viewerNo);
        chatRedisService.updateReadYn(roomNo, viewerNo);

        broadcastToRoom(roomNo, Map.of(
                "type", "READ",
                "roomNo", roomNo,
                "viewerNo", viewerNo
        ));
    }

    /**
     * 상담 종료 이벤트 전송
     */
    public void broadcastClose(Integer roomNo, ChatMessageDto closeMessage) {
        broadcastToRoom(roomNo, Map.of(
                "type", "CLOSE",
                "roomNo", roomNo,
                "message", closeMessage
        ));
    }

    /**
     * 관리자 상담 목록 갱신 이벤트 전송
     */
    public void broadcastAdminListRefresh(Integer roomNo) {
        broadcastToAdminList(Map.of(
                "type", "ADMIN_ROOM_REFRESH",
                "roomNo", roomNo
        ));
    }

    /**
     * 특정 채팅방에 접속 중인 세션들에게 JSON 메시지 전송
     */
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

    /**
     * 관리자 상담 목록 페이지에 접속 중인 세션들에게 JSON 메시지 전송
     */
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

    /**
     * 상담방이 OPEN 상태인지 확인
     */
    private boolean isOpenRoom(Integer roomNo) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        return chatRoom != null && "OPEN".equals(chatRoom.getStatus());
    }

    private Integer getInteger(JsonNode root, String fieldName) {
        if (!root.has(fieldName) || root.get(fieldName) == null || root.get(fieldName).isNull()) {
            return null;
        }

        return root.get(fieldName).asInt();
    }

    /**
     * WebSocket 연결 종료 시 등록된 세션 정보를 정리한다.
     */
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