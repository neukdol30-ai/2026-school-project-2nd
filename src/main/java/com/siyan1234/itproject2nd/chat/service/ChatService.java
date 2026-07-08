package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import org.springframework.scheduling.support.SimpleTriggerContext;
import org.springframework.stereotype.Service;

import java.util.List;

public interface ChatService {

    ChatRoomDto getOrCreateRoom(Integer userNo);

    //1:1채팅 문의기능
    ChatRoomDto getOrCreateRoom(Integer userNo, String category);

    ChatRoomDto findOpenRoomByUserNo(Integer userNo);

    ChatRoomDto findRoomByRoomNo(Integer roomNo);

    List<ChatRoomDto> findAllRooms();

    void saveMessage(ChatMessageDto chatMessageDto);

    List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo);

    void closeRoom(Integer roomNo);

    void updateLastMessage(Integer roomNo, String lastMessage);

    void updateReadYn(Integer roomNo, Integer viewerNo);

    List<ChatRoomDto> findAdminRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    );

    int countAdminRooms(
            String status,
            String category,
            String keyword
    );
}
