package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
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
 * 보안 보완:
 * - 클라이언트가 보낸 senderNo, viewerNo를 신뢰하지 않는다.
 * - WebSocketSession의 로그인 사용자 정보를 기준으로 senderNo/viewerNo를 결정한다.
 * - JOIN, READ, MESSAGE 처리 시 상담방 접근 권한을 검사한다.
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
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            closeUnauthorizedSession(session);
            return;
        }

        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);

        String type = root.has("type") ? root.get("type").asText() : "MESSAGE";

        if ("ADMIN_LIST_JOIN".equals(type)) {
            handleAdminListJoin(session, loginUser);
            return;
        }

        if ("JOIN".equals(type)) {
            handleJoin(session, root, loginUser);
            return;
        }

        if ("READ".equals(type)) {
            handleRead(session, root, loginUser);
            return;
        }

        if ("MESSAGE".equals(type)) {
            handleMessage(session, root, loginUser);
        }
    }

    /**
     * 관리자 상담 목록 페이지 접속 등록
     *
     * 관리자 목록 실시간 갱신은 ADMIN만 받을 수 있도록 검사한다.
     */
    private void handleAdminListJoin(WebSocketSession session, MemberDto loginUser) throws IOException {
        if (!isAdmin(loginUser)) {
            closeUnauthorizedSession(session);
            return;
        }

        adminListSessions.add(session);
        log.info("관리자 상담 목록 WebSocket 등록 sessionId={}", session.getId());
    }

    /**
     * 채팅방 입장 처리
     *
     * 중요:
     * - 클라이언트가 보낸 viewerNo를 사용하지 않는다.
     * - 서버가 확인한 loginUser.getNo()를 viewerNo로 사용한다.
     */
    private void handleJoin(
            WebSocketSession session,
            JsonNode root,
            MemberDto loginUser
    ) throws IOException {
        Integer roomNo = getInteger(root, "roomNo");

        if (roomNo == null) {
            return;
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!canAccessRoom(loginUser, chatRoom)) {
            closeUnauthorizedSession(session);
            return;
        }

        joinRoom(roomNo, session);

        Integer viewerNo = loginUser.getNo();

        updateReadAndBroadcast(roomNo, viewerNo);
        broadcastAdminListRefresh(roomNo);
    }

    /**
     * 읽음 처리 이벤트
     *
     * 중요:
     * - 클라이언트가 보낸 viewerNo를 사용하지 않는다.
     * - 서버가 확인한 loginUser.getNo()를 viewerNo로 사용한다.
     */
    private void handleRead(
            WebSocketSession session,
            JsonNode root,
            MemberDto loginUser
    ) throws IOException {
        Integer roomNo = getInteger(root, "roomNo");

        if (roomNo == null) {
            return;
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!canAccessRoom(loginUser, chatRoom)) {
            closeUnauthorizedSession(session);
            return;
        }

        Integer viewerNo = loginUser.getNo();

        updateReadAndBroadcast(roomNo, viewerNo);
        broadcastAdminListRefresh(roomNo);
    }

    /**
     * 메시지 전송 이벤트
     *
     * 보안 핵심:
     * - 클라이언트가 보낸 senderNo를 사용하지 않는다.
     * - 서버가 확인한 loginUser.getNo()를 senderNo로 강제 설정한다.
     * - 본인 상담방이 아니면 WebSocket 연결을 종료한다.
     * - 종료된 상담방에는 메시지를 저장하지 않는다.
     */
    private void handleMessage(
            WebSocketSession session,
            JsonNode root,
            MemberDto loginUser
    ) throws Exception {
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

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!canAccessRoom(loginUser, chatRoom)) {
            closeUnauthorizedSession(session);
            return;
        }

        if (!"OPEN".equals(chatRoom.getStatus())) {
            return;
        }

        String messageContent = chatMessageDto.getMessageContent();

        if (messageContent == null || messageContent.trim().isEmpty()) {
            return;
        }

        /*
            중요:
            클라이언트가 보낸 senderNo는 조작될 수 있으므로 사용하지 않는다.
            반드시 서버의 로그인 사용자 번호로 설정한다.
         */
        chatMessageDto.setSenderNo(loginUser.getNo());
        chatMessageDto.setMessageContent(messageContent.trim());
        chatMessageDto.setReadYn("N");
        chatMessageDto.setCreatedDate(LocalDateTime.now());

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
     * WebSocketSession에서 로그인 사용자 정보를 가져온다.
     *
     * Spring Security 로그인 상태라면
     * session.getPrincipal() 안에 Authentication 정보가 들어온다.
     */
    private MemberDto getLoginUser(WebSocketSession session) {
        if (session == null) {
            return null;
        }

        Object principal = session.getPrincipal();

        if (principal == null) {
            return null;
        }

        if (principal instanceof Authentication authentication) {
            if (!authentication.isAuthenticated()) {
                return null;
            }

            if (authentication instanceof AnonymousAuthenticationToken) {
                return null;
            }

            Object authenticationPrincipal = authentication.getPrincipal();

            if (authenticationPrincipal instanceof CustomUserDetails customUserDetails) {
                return customUserDetails.getMemberDto();
            }

            return null;
        }

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMemberDto();
        }

        return null;
    }

    /**
     * 권한 없는 WebSocket 요청이면 연결을 종료한다.
     */
    private void closeUnauthorizedSession(WebSocketSession session) throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized chat room access"));
        }
    }

    /**
     * 상담방 접근 권한 검사
     *
     * 관리자:
     * - 모든 상담방 접근 가능
     *
     * 일반 사용자:
     * - 본인이 생성한 상담방만 접근 가능
     */
    private boolean canAccessRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        if (isAdmin(loginUser)) {
            return true;
        }

        return chatRoom.getUserNo() != null
                && chatRoom.getUserNo().equals(loginUser.getNo());
    }

    /**
     * 관리자 여부 확인
     */
    private boolean isAdmin(MemberDto loginUser) {
        return loginUser != null && "ADMIN".equals(loginUser.getRole());
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