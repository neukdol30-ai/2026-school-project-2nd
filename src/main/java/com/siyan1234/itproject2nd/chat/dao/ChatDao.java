package com.siyan1234.itproject2nd.chat.dao;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatDao {

    int createRoom(ChatRoomDto chatRoomDto);

    ChatRoomDto findRoomByUserNo(Integer userNo);

    ChatRoomDto findRoomByRoomNo(Integer roomNo);

    List<ChatRoomDto> findAllRooms();

    //1:1채팅 문의 기능
    int updateRoomCategory(
            @Param("roomNo") Integer roomNo,
            @Param("category") String category
    );

    int updateLastMessage(ChatRoomDto chatRoomDto);

    int closeRoom(Integer roomNo);

    int saveMessage(ChatMessageDto chatMessageDto);

    List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo);

    int updateReadYn(
            @Param("roomNo") Integer roomNo,
            @Param("viewerNo") Integer viewerNo
    );
}
