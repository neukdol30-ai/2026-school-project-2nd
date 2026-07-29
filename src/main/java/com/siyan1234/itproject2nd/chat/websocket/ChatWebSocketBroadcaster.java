package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Set;

/**
 * 채팅 WebSocket 이벤트 전송을 담당합니다.
 *
 * Handler 외부 Service에서 실시간 갱신을 호출할 때도
 * ChatHandler에 직접 의존하지 않고 이 클래스를 사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketBroadcaster {

    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public void broadcastMessage(Integer roomNo, ChatMessageDto chatMessageDto) {
        broadcastToRoom(roomNo, Map.of(
                "type", ChatWebSocketEventType.MESSAGE,
                "message", chatMessageDto
        ));
    }

    public void broadcastRead(Integer roomNo, Integer viewerNo) {
        broadcastToRoom(roomNo, Map.of(
                "type", ChatWebSocketEventType.READ,
                "roomNo", roomNo,
                "viewerNo", viewerNo
        ));
    }

    public void broadcastClose(Integer roomNo, ChatMessageDto closeMessage) {
        broadcastToRoom(roomNo, Map.of(
                "type", ChatWebSocketEventType.CLOSE,
                "roomNo", roomNo,
                "message", closeMessage
        ));
    }

    public void broadcastAdminListRefresh(Integer roomNo) {
        broadcastToAdminList(Map.of(
                "type", ChatWebSocketEventType.ADMIN_ROOM_REFRESH,
                "roomNo", roomNo
        ));
    }

    public void broadcastToRoom(Integer roomNo, Object data) {
        Set<WebSocketSession> sessions = sessionRegistry.getRoomSessions(roomNo);

        if (sessions.isEmpty()) {
            return;
        }

        sendToSessions(sessions, data, "WebSocket 채팅방 메시지 전송 실패 roomNo=" + roomNo);
    }

    private void broadcastToAdminList(Object data) {
        Set<WebSocketSession> sessions = sessionRegistry.getAdminListSessions();

        if (sessions.isEmpty()) {
            return;
        }

        sendToSessions(sessions, data, "관리자 상담 목록 WebSocket 전송 실패");
    }

    private void sendToSessions(Set<WebSocketSession> sessions, Object data, String errorMessage) {
        try {
            String json = objectMapper.writeValueAsString(data);

            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                } else {
                    sessionRegistry.removeSession(session);
                }
            }
        } catch (Exception e) {
            log.error(errorMessage, e);
        }
    }
}
