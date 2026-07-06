package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dao.ChatDao;
import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatDao chatDao;

    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo) {
        ChatRoomDto findRoom = chatDao.findRoomByUserNo(userNo);

        if (findRoom != null) {
            return findRoom;
        }

        ChatRoomDto newRoom = new ChatRoomDto();
        newRoom.setUserNo(userNo);
        newRoom.setAdminNo(null);

        chatDao.createRoom(newRoom);

        return chatDao.findRoomByUserNo(userNo);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDto findRoomByRoomNo(Integer roomNo) {
        return chatDao.findRoomByRoomNo(roomNo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> findAllRooms() {
        return chatDao.findAllRooms();
    }

    @Override
    public void saveMessage(ChatMessageDto chatMessageDto) {
        chatDao.saveMessage(chatMessageDto);

        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomNo(chatMessageDto.getRoomNo());
        chatRoomDto.setLastMessage(chatMessageDto.getMessageContent());

        chatDao.updateLastMessage(chatRoomDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo) {
        return chatDao.findMessagesByRoomNo(roomNo);
    }

    @Override
    public void closeRoom(Integer roomNo) {
        chatDao.closeRoom(roomNo);
    }

    @Override
    public void updateReadYn(Integer roomNo) {
        chatDao.updateReadYn(roomNo);
    }


}
