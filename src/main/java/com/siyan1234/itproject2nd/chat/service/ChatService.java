package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ChatService {
    ChatRoomDto getOrCreateRoom(Integer userNo);
    ChatRoomDto findRoomByRoomNo(Integer roomNo);
    List<ChatRoomDto> findAllRooms();
    void saveMessage(ChatMessageDto chatMessageDto);
    List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo);
    void closeRoom(Integer roomNo);
    void updateReadYn(Integer roomNo);
}
