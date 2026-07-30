package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dao.ChatDao;
import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.support.ChatCategory;
import com.siyan1234.itproject2nd.chat.support.ChatMessagePolicy;
import com.siyan1234.itproject2nd.chat.support.ChatReadStatus;
import com.siyan1234.itproject2nd.chat.support.ChatRoomLockManager;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatService의 실제 구현 클래스입니다.
 *
 * 상담방 생성, 메시지 저장, 관리자 목록 조회, 읽음 처리,
 * 상담 종료, 삭제, 문의 유형 변경 등의 비즈니스 로직을 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private static final int MESSAGE_BATCH_SIZE = 100;

    private final ChatDao chatDao;
    private final ChatRedisService chatRedisService;
    private final ChatRoomLockManager roomLockManager;

    /** 문의 유형이 없는 요청은 기본 유형인 ETC로 처리합니다. */
    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo) {
        return getOrCreateRoom(userNo, ChatCategory.DEFAULT);
    }

    /**
     * 사용자의 OPEN 상담방을 조회하거나 새로 생성합니다.
     * 이미 OPEN 상담방이 있으면 새 방을 만들지 않고 문의 유형만 갱신합니다.
     */
    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo, String category) {
        category = normalizeCategory(category);

        ChatRoomDto findRoom = chatDao.findRoomByUserNo(userNo);

        if (findRoom != null) {
            if (!category.equals(findRoom.getCategory())) {
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

    /**
     * HTTP 테스트 API처럼 Redis를 거치지 않는 단건 메시지를 Oracle에 저장합니다.
     * 같은 상담방의 읽음 처리와 저장 순서가 뒤섞이지 않도록 상담방 Lock 안에서 실행합니다.
     */
    @Override
    public void saveMessage(ChatMessageDto chatMessageDto) {
        if (!isValidMessage(chatMessageDto)) {
            return;
        }

        roomLockManager.execute(chatMessageDto.getRoomNo(), () -> {
            normalizeMessage(chatMessageDto);
            chatDao.saveMessage(chatMessageDto);
            updateLastMessageInternal(
                    chatMessageDto.getRoomNo(),
                    chatMessageDto.getMessageContent(),
                    chatMessageDto.getCreatedDate()
            );
        });
    }

    /**
     * Scheduler가 한 상담방의 Redis 메시지를 Oracle 익명 PL/SQL 블록으로 묶어서 저장합니다.
     * 모든 메시지가 유효해야만 SQL을 실행하며 마지막 메시지 정보는 한 번만 갱신합니다.
     */
    @Override
    public int saveMessages(List<ChatMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }

        Integer roomNo = messages.getFirst() == null ? null : messages.getFirst().getRoomNo();
        if (roomNo == null) {
            throw new IllegalArgumentException("저장할 채팅 메시지에 상담방 번호가 없습니다.");
        }

        return roomLockManager.execute(roomNo, () -> {
            for (ChatMessageDto message : messages) {
                if (!isValidMessage(message) || !roomNo.equals(message.getRoomNo())) {
                    throw new IllegalArgumentException("같은 상담방의 유효한 메시지만 일괄 저장할 수 있습니다.");
                }

                normalizeMessage(message);
            }

            for (int start = 0; start < messages.size(); start += MESSAGE_BATCH_SIZE) {
                int end = Math.min(start + MESSAGE_BATCH_SIZE, messages.size());
                chatDao.saveMessages(messages.subList(start, end));
            }

            ChatMessageDto lastMessage = messages.getLast();
            updateLastMessageInternal(
                    roomNo,
                    lastMessage.getMessageContent(),
                    lastMessage.getCreatedDate()
            );

            return messages.size();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo) {
        return chatDao.findMessagesByRoomNo(roomNo);
    }

    @Override
    public void closeRoom(Integer roomNo) {
        if (roomNo != null) {
            chatDao.closeRoom(roomNo);
        }
    }

    @Override
    public void deleteRoom(Integer roomNo) {
        if (roomNo != null) {
            chatDao.deleteRoom(roomNo);
        }
    }

    @Override
    public int deleteClosedRooms(List<Integer> roomNoList) {
        if (roomNoList == null || roomNoList.isEmpty()) {
            return 0;
        }

        return chatDao.deleteClosedRooms(roomNoList);
    }

    /**
     * Oracle과 Redis의 상대방 메시지를 하나의 상담방 Lock 안에서 함께 읽음 처리합니다.
     *
     * Scheduler가 Redis 메시지를 DB로 옮기는 순간과 읽음 처리가 겹쳐
     * 이미 읽은 메시지가 DB에 N으로 들어가는 경쟁 상태를 방지합니다.
     */
    @Override
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        if (roomNo == null || viewerNo == null) {
            return;
        }

        roomLockManager.execute(roomNo, () -> {
            chatDao.updateReadYn(roomNo, viewerNo);
            chatRedisService.updateReadYn(roomNo, viewerNo);
        });
    }

    /**
     * 관리자 상담 목록의 DB 안읽음 개수와 Redis Hash 카운터를 합산합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> findAdminRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    ) {
        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
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
            int redisUnreadCount = chatRedisService.countUnreadMessages(room.getRoomNo(), viewerNo);
            room.setUnreadCount(dbUnreadCount + redisUnreadCount);
        }

        return roomList;
    }

    @Override
    @Transactional(readOnly = true)
    public int countAdminRooms(String status, String category, String keyword) {
        return chatDao.countAdminRooms(status, category, keyword);
    }

    @Override
    public void updateLastMessage(Integer roomNo, String lastMessage) {
        updateLastMessage(roomNo, lastMessage, null);
    }

    @Override
    public void updateLastMessage(
            Integer roomNo,
            String lastMessage,
            LocalDateTime lastMessageDate
    ) {
        if (roomNo == null) {
            return;
        }

        updateLastMessageInternal(roomNo, lastMessage, lastMessageDate);
    }

    @Override
    public void assignAdmin(Integer roomNo, Integer adminNo) {
        if (roomNo != null && adminNo != null) {
            chatDao.assignAdmin(roomNo, adminNo);
        }
    }

    /**
     * OPEN 상태이며 로그인 사용자가 소유한 상담방만 문의 유형을 변경합니다.
     */
    @Override
    public void changeCategory(Integer roomNo, Integer userNo, String category) {
        category = normalizeCategory(category);

        ChatRoomDto chatRoom = chatDao.findRoomByRoomNo(roomNo);

        if (chatRoom == null
                || !ChatRoomStatus.isOpen(chatRoom.getStatus())
                || chatRoom.getUserNo() == null
                || !chatRoom.getUserNo().equals(userNo)) {
            return;
        }

        chatDao.updateRoomCategory(roomNo, category);
    }

    /**
     * null, 빈 문자열, 허용되지 않은 문의 유형은 ETC로 정규화합니다.
     */
    private String normalizeCategory(String category) {
        return ChatCategory.normalize(category);
    }

    /**
     * 사용자 본인의 상담내역 목록에 DB와 Redis의 안읽음 개수를 합산합니다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> findUserRooms(Integer userNo, int page, int size) {
        if (userNo == null) {
            return List.of();
        }

        if (page < 1) {
            page = 1;
        }
        if (size < 1) {
            size = 10;
        }

        int pageStart = (page - 1) * size;
        int pageEnd = size;

        List<ChatRoomDto> roomList = chatDao.findUserRooms(userNo, pageStart, pageEnd);

        for (ChatRoomDto room : roomList) {
            int dbUnreadCount = room.getUnreadCount() == null ? 0 : room.getUnreadCount();
            int redisUnreadCount = chatRedisService.countUnreadMessages(room.getRoomNo(), userNo);
            room.setUnreadCount(dbUnreadCount + redisUnreadCount);
        }

        return roomList;
    }

    @Override
    @Transactional(readOnly = true)
    public int countUserRooms(Integer userNo) {
        if (userNo == null) {
            return 0;
        }
        return chatDao.countUserRooms(userNo);
    }

    private boolean isValidMessage(ChatMessageDto message) {
        return message != null
                && message.getRoomNo() != null
                && ChatMessagePolicy.isValid(message.getMessageContent());
    }

    private void normalizeMessage(ChatMessageDto message) {
        if (message.getCreatedDate() == null) {
            message.setCreatedDate(LocalDateTime.now());
        }

        message.setMessageContent(
                ChatMessagePolicy.normalizeAndValidate(message.getMessageContent())
        );
        message.setReadYn(ChatReadStatus.normalize(message.getReadYn()));
    }

    private void updateLastMessageInternal(
            Integer roomNo,
            String lastMessage,
            LocalDateTime lastMessageDate
    ) {
        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomNo(roomNo);
        chatRoomDto.setLastMessage(lastMessage);
        chatRoomDto.setLastMessageDate(lastMessageDate);
        chatDao.updateLastMessage(chatRoomDto);
    }
}
