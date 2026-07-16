package com.siyan1234.itproject2nd.chat.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 채팅 WebSocket 세션 저장소입니다.
 *
 * ChatHandler가 직접 Map/Set을 관리하지 않도록 분리했습니다.
 * - 상담방별 접속 세션
 * - 세션이 입장한 상담방 번호
 * - 관리자 상담 목록 페이지 접속 세션
 */
@Component
public class ChatWebSocketSessionRegistry {

    private final Map<Integer, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> sessionRoomMap = new ConcurrentHashMap<>();
    private final Set<WebSocketSession> adminListSessions = ConcurrentHashMap.newKeySet();

    public void addAdminListSession(WebSocketSession session) {
        if (session == null) {
            return;
        }
        adminListSessions.add(session);
    }

    public void joinRoom(Integer roomNo, WebSocketSession session) {
        if (roomNo == null || session == null) {
            return;
        }

        roomSessionMap
                .computeIfAbsent(roomNo, key -> ConcurrentHashMap.newKeySet())
                .add(session);

        sessionRoomMap.put(session.getId(), roomNo);
    }

    public Set<WebSocketSession> getRoomSessions(Integer roomNo) {
        if (roomNo == null) {
            return Set.of();
        }

        Set<WebSocketSession> sessions = roomSessionMap.get(roomNo);

        if (sessions == null || sessions.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(sessions);
    }

    public Set<WebSocketSession> getAdminListSessions() {
        if (adminListSessions.isEmpty()) {
            return Set.of();
        }

        return new HashSet<>(adminListSessions);
    }

    public void removeSession(WebSocketSession session) {
        if (session == null) {
            return;
        }

        adminListSessions.remove(session);

        Integer roomNo = sessionRoomMap.remove(session.getId());

        if (roomNo == null) {
            return;
        }

        Set<WebSocketSession> sessions = roomSessionMap.get(roomNo);

        if (sessions == null) {
            return;
        }

        sessions.remove(session);

        if (sessions.isEmpty()) {
            roomSessionMap.remove(roomNo);
        }
    }
}
