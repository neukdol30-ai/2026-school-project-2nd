package com.siyan1234.itproject2nd.chat.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/** 채팅 실시간 통신 endpoint와 개발 환경 허용 Origin을 등록합니다. */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChatHandler chatHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatHandler, "/ws/chat")
                // 운영 배포 시에는 실제 서비스 도메인만 허용하도록 목록을 교체해야 합니다.
                .setAllowedOriginPatterns(
                        "http://localhost:8080",
                        "http://127.0.0.1:8080",
                        "http://localhost:5173"
                );
    }
}