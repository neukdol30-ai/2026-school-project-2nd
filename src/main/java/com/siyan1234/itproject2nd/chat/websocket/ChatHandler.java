package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.support.ChatMessagePolicy;
import com.siyan1234.itproject2nd.chat.support.ChatReadStatus;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * WebSocket 실시간 채팅 이벤트 처리 클래스입니다.
 *
 * 이 클래스는 WebSocket 메시지의 흐름 제어만 담당합니다.
 * - 세션 저장: ChatWebSocketSessionRegistry
 * - 인증/권한 검사: ChatWebSocketAuthService
 * - JSON 파싱: ChatWebSocketPayloadParser
 * - 실시간 이벤트 전송: ChatWebSocketBroadcaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatRedisService chatRedisService;
    private final ChatService chatService;
    private final ChatWebSocketSessionRegistry sessionRegistry;
    private final ChatWebSocketAuthService authService;
    private final ChatWebSocketPayloadParser payloadParser;
    private final ChatWebSocketBroadcaster broadcaster;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 연결 성공 sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        MemberDto loginUser = authService.getLoginUser(session);

        if (loginUser == null) {
            authService.closeUnauthorizedSession(session);
            return;
        }

        if (message.getPayloadLength() > ChatMessagePolicy.MAX_WEBSOCKET_PAYLOAD_LENGTH) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.MESSAGE_TOO_LONG_CODE,
                    ChatMessagePolicy.MESSAGE_TOO_LONG_TEXT
            );
            return;
        }

        JsonNode root;

        try {
            root = payloadParser.readTree(message.getPayload());
        } catch (Exception e) {
            log.debug("올바르지 않은 WebSocket JSON payload sessionId={}", session.getId());
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.INVALID_PAYLOAD_CODE,
                    ChatMessagePolicy.INVALID_PAYLOAD_TEXT
            );
            return;
        }

        String type = payloadParser.getType(root);

        if (type == null || type.isBlank()) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.INVALID_PAYLOAD_CODE,
                    ChatMessagePolicy.INVALID_PAYLOAD_TEXT
            );
            return;
        }

        switch (type) {
            case ChatWebSocketEventType.ADMIN_LIST_JOIN -> handleAdminListJoin(session, loginUser);
            case ChatWebSocketEventType.JOIN -> handleJoin(session, root, loginUser);
            case ChatWebSocketEventType.READ -> handleRead(session, root, loginUser);
            case ChatWebSocketEventType.MESSAGE -> handleMessage(session, root, loginUser);
            default -> {
                log.debug("지원하지 않는 WebSocket 이벤트 type={}", type);
                broadcaster.sendError(
                        session,
                        ChatMessagePolicy.INVALID_PAYLOAD_CODE,
                        ChatMessagePolicy.INVALID_PAYLOAD_TEXT
                );
            }
        }
    }

    private void handleAdminListJoin(WebSocketSession session, MemberDto loginUser) throws IOException {
        if (!authService.isAdmin(loginUser)) {
            authService.closeUnauthorizedSession(session);
            return;
        }

        sessionRegistry.addAdminListSession(session);
        log.info("관리자 상담 목록 WebSocket 등록 sessionId={}", session.getId());
    }

    private void handleJoin(WebSocketSession session, JsonNode root, MemberDto loginUser) throws IOException {
        Integer roomNo = payloadParser.getInteger(root, "roomNo");

        if (roomNo == null) {
            return;
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!authService.canAccessRoom(loginUser, chatRoom)) {
            authService.closeUnauthorizedSession(session);
            return;
        }

        sessionRegistry.joinRoom(roomNo, session);
        updateReadAndBroadcast(roomNo, loginUser.getNo());
        broadcaster.broadcastAdminListRefresh(roomNo);
    }

    private void handleRead(WebSocketSession session, JsonNode root, MemberDto loginUser) throws IOException {
        Integer roomNo = payloadParser.getInteger(root, "roomNo");

        if (roomNo == null) {
            return;
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!authService.canAccessRoom(loginUser, chatRoom)) {
            authService.closeUnauthorizedSession(session);
            return;
        }

        updateReadAndBroadcast(roomNo, loginUser.getNo());
        broadcaster.broadcastAdminListRefresh(roomNo);
    }

    private void handleMessage(WebSocketSession session, JsonNode root, MemberDto loginUser) throws Exception {
        ChatMessageDto chatMessageDto;

        try {
            chatMessageDto = payloadParser.parseChatMessage(root);
        } catch (Exception e) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.INVALID_PAYLOAD_CODE,
                    ChatMessagePolicy.INVALID_PAYLOAD_TEXT
            );
            return;
        }

        if (chatMessageDto == null || chatMessageDto.getRoomNo() == null) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.INVALID_PAYLOAD_CODE,
                    ChatMessagePolicy.INVALID_PAYLOAD_TEXT
            );
            return;
        }

        Integer roomNo = chatMessageDto.getRoomNo();

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!authService.canAccessRoom(loginUser, chatRoom)) {
            authService.closeUnauthorizedSession(session);
            return;
        }

        if (!ChatRoomStatus.isOpen(chatRoom.getStatus())) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.CHAT_CLOSED_CODE,
                    ChatMessagePolicy.CHAT_CLOSED_TEXT
            );
            return;
        }

        String messageContent = ChatMessagePolicy.normalize(chatMessageDto.getMessageContent());

        if (ChatMessagePolicy.isBlank(messageContent)) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.EMPTY_MESSAGE_CODE,
                    ChatMessagePolicy.EMPTY_MESSAGE_TEXT
            );
            return;
        }

        if (ChatMessagePolicy.exceedsMaxLength(messageContent)) {
            broadcaster.sendError(
                    session,
                    ChatMessagePolicy.MESSAGE_TOO_LONG_CODE,
                    ChatMessagePolicy.MESSAGE_TOO_LONG_TEXT
            );
            return;
        }

        prepareMessage(chatMessageDto, loginUser.getNo(), messageContent);
        sessionRegistry.joinRoom(roomNo, session);

        // 메시지는 Redis에 먼저 저장하고, Scheduler가 Oracle DB로 옮깁니다.
        chatRedisService.saveMessage(chatMessageDto);

        // 관리자 목록 정렬과 마지막 메시지 표시를 위해 chat_room은 즉시 갱신합니다.
        chatService.updateLastMessage(
                roomNo,
                chatMessageDto.getMessageContent(),
                chatMessageDto.getCreatedDate()
        );

        broadcaster.broadcastMessage(roomNo, chatMessageDto);
        broadcaster.broadcastAdminListRefresh(roomNo);
    }

    private void prepareMessage(ChatMessageDto chatMessageDto, Integer senderNo, String messageContent) {
        chatMessageDto.setSenderNo(senderNo);
        chatMessageDto.setMessageContent(messageContent);
        chatMessageDto.setReadYn(ChatReadStatus.UNREAD);
        chatMessageDto.setCreatedDate(LocalDateTime.now());
    }

    private void updateReadAndBroadcast(Integer roomNo, Integer viewerNo) {
        // ChatService가 Oracle과 Redis를 같은 상담방 Lock 안에서 함께 읽음 처리합니다.
        chatService.updateReadYn(roomNo, viewerNo);
        broadcaster.broadcastRead(roomNo, viewerNo);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket 연결 종료 sessionId={}", session.getId());
        sessionRegistry.removeSession(session);
    }
}
