package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.net.http.WebSocket;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final ChatRedisService chatRedisService;
    private final ObjectMapper objectMapper;

    //roomNo별 접속자 목록
    private final Map<Integer, Set<WebSocketSession>> roomSessionMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session){
        System.out.println("WebSocket 연결 성공: "+session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception{
        String payload = message.getPayload();

        ChatMessageDto chatMessageDto =
                objectMapper.readValue(payload, ChatMessageDto.class);

        //1:1문의 채팅 시간표시
        chatMessageDto.setCreatedDate(LocalDateTime.now());

        Integer roomNo = chatMessageDto.getRoomNo();

        //방 세션 목록에 현재 세션 추가
        roomSessionMap
                .computeIfAbsent(roomNo, key -> ConcurrentHashMap.newKeySet())
                .add(session);
        // DB에 메시지 저장
        chatRedisService.saveMessage(chatMessageDto);

        // 같은 채팅방 접속자들에게 메시지 전송
        String sendMessage = objectMapper.writeValueAsString(chatMessageDto);

        Set<WebSocketSession> sessions = roomSessionMap.get(roomNo);

        if (sessions != null){
            for (WebSocketSession webSocketSession : sessions){
                if (webSocketSession.isOpen()){
                    webSocketSession.sendMessage(new TextMessage(sendMessage));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status){
        System.out.println("WebSocket 연결 종료" + session.getId());

        for (Set<WebSocketSession> sessions : roomSessionMap.values()){
            sessions.remove(session);
        }
    }
}
