package com.siyan1234.itproject2nd.chat.support;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;

import java.time.LocalDateTime;

/**
 * 시스템 메시지나 기본 채팅 메시지 DTO 생성을 담당합니다.
 */
public final class ChatMessageFactory {

    private static final String CLOSE_MESSAGE = "상담이 종료되었습니다.";

    private ChatMessageFactory() {
    }

    public static ChatMessageDto closeMessage(Integer roomNo, Integer adminNo) {
        ChatMessageDto closeMessage = new ChatMessageDto();
        closeMessage.setRoomNo(roomNo);
        closeMessage.setSenderNo(adminNo);
        closeMessage.setMessageContent(CLOSE_MESSAGE);
        closeMessage.setReadYn(ChatReadStatus.UNREAD);
        closeMessage.setCreatedDate(LocalDateTime.now());
        return closeMessage;
    }
}
