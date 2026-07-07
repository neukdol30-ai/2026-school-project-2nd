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
    private final ChatRedisService chatRedisService;

    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo) {
        return getOrCreateRoom(userNo, "ETC");
    }

    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo, String category) {
        ChatRoomDto findRoom = chatDao.findRoomByUserNo(userNo);

        if (findRoom != null) {
            if (category != null && !category.equals(findRoom.getCategory())) {
                chatDao.updateRoomCategory(findRoom.getRoomNo(), category);
                findRoom.setCategory(category);
            }

            return findRoom;
        }

        ChatRoomDto newRoom = new ChatRoomDto();
        newRoom.setUserNo(userNo);
        newRoom.setAdminNo(null);
        newRoom.setCategory(category);

        chatDao.createRoom(newRoom);

        return chatDao.findRoomByUserNo(userNo);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomDto findOpenRoomByUserNo(Integer userNo) {
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
        if (chatMessageDto.getCreatedDate() == null) {
            chatMessageDto.setCreatedDate(java.time.LocalDateTime.now());
        }
        if (chatMessageDto.getReadYn() == null) {
            chatMessageDto.setReadYn("N");
        }

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
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        chatDao.updateReadYn(roomNo, viewerNo);
    }

    @Override
    public List<ChatRoomDto> findAdminRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    ){
        if (page < 1){
            page = 1;
        }
        if (size < 1){
            size = 10;
        }
        int offset = (page - 1) * size;

        List<ChatRoomDto> roomList = chatDao.findAdminRooms(
                status,
                category,
                keyword,
                viewerNo,
                offset,
                size
        );
        for (ChatRoomDto room : roomList) {
            int dbUnreadCount = room.getUnreadCount() == null ? 0 : room.getUnreadCount();
            int redisUnreadCount = chatRedisService.countUnreadMessages(room.getRoomNo(),viewerNo);

            room.setUnreadCount(dbUnreadCount + redisUnreadCount);
        }
        return roomList;
    }
    @Override
    public int countAdminRooms(String status, String category, String keyword) {
        return chatDao.countAdminRooms(status, category, keyword);
    }
}
