package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dao.ChatDao;
import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ChatService의 실제 구현 클래스
 *
 * 상담방 생성, 메시지 저장, 관리자 목록 조회, 읽음 처리,
 * 상담 종료, 삭제, 문의 유형 변경 등의 비즈니스 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatDao chatDao;
    private final ChatRedisService chatRedisService;

    /**
     * 문의 유형이 없는 기본 상담방 생성 메서드
     * 기본 문의 유형을 ETC로 설정해서 category 포함 메서드로 위임한다.
     */
    @Override
    public ChatRoomDto getOrCreateRoom(Integer userNo) {
        return getOrCreateRoom(userNo, "ETC");
    }

    /**
     * 사용자의 OPEN 상담방을 조회하거나 새로 생성한다.
     *
     * 이미 OPEN 상담방이 있으면 새 방을 만들지 않고 기존 방을 반환한다.
     * 사용자가 문의 유형을 다르게 선택한 경우 기존 방의 category만 변경한다.
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

    /**
     * 사용자의 진행 중인 OPEN 상담방 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ChatRoomDto findOpenRoomByUserNo(Integer userNo) {
        return chatDao.findRoomByUserNo(userNo);
    }

    /**
     * roomNo로 상담방 상세 조회
     */
    @Override
    @Transactional(readOnly = true)
    public ChatRoomDto findRoomByRoomNo(Integer roomNo) {
        return chatDao.findRoomByRoomNo(roomNo);
    }

    /**
     * 전체 상담방 조회
     * Scheduler가 Redis 메시지를 Oracle로 저장할 때 사용한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDto> findAllRooms() {
        return chatDao.findAllRooms();
    }

    /**
     * 메시지를 Oracle DB에 저장한다.
     *
     * Redis에 임시 저장된 메시지를 Scheduler가 Oracle로 옮길 때 사용한다.
     * 메시지 저장 후 관리자 목록에 표시할 마지막 메시지도 함께 갱신한다.
     */
    @Override
    public void saveMessage(ChatMessageDto chatMessageDto) {
        if (chatMessageDto == null) {
            return;
        }

        if (chatMessageDto.getRoomNo() == null) {
            return;
        }

        if (chatMessageDto.getMessageContent() == null || chatMessageDto.getMessageContent().isBlank()) {
            return;
        }

        if (chatMessageDto.getCreatedDate() == null) {
            chatMessageDto.setCreatedDate(LocalDateTime.now());
        }

        if (!"Y".equals(chatMessageDto.getReadYn()) && !"N".equals(chatMessageDto.getReadYn())) {
            chatMessageDto.setReadYn("N");
        }

        chatDao.saveMessage(chatMessageDto);

        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomNo(chatMessageDto.getRoomNo());
        chatRoomDto.setLastMessage(chatMessageDto.getMessageContent());

        chatDao.updateLastMessage(chatRoomDto);
    }

    /**
     * Oracle DB에 저장된 특정 상담방 메시지 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageDto> findMessagesByRoomNo(Integer roomNo) {
        return chatDao.findMessagesByRoomNo(roomNo);
    }

    /**
     * 상담방 상태를 CLOSED로 변경한다.
     */
    @Override
    public void closeRoom(Integer roomNo) {
        if (roomNo == null) {
            return;
        }

        chatDao.closeRoom(roomNo);
    }

    /**
     * 종료된 상담방 단건 하드 DELETE
     */
    @Override
    public void deleteRoom(Integer roomNo) {
        if (roomNo == null) {
            return;
        }

        chatDao.deleteRoom(roomNo);
    }

    /**
     * 종료된 상담방 다중 하드 DELETE
     */
    @Override
    public int deleteClosedRooms(List<Integer> roomNoList) {
        if (roomNoList == null || roomNoList.isEmpty()) {
            return 0;
        }

        return chatDao.deleteClosedRooms(roomNoList);
    }

    /**
     * 현재 접속자가 상대방 메시지를 읽은 것으로 처리한다.
     */
    @Override
    public void updateReadYn(Integer roomNo, Integer viewerNo) {
        if (roomNo == null || viewerNo == null) {
            return;
        }

        chatDao.updateReadYn(roomNo, viewerNo);
    }

    /**
     * 관리자 상담 목록 조회
     *
     * Oracle DB에 저장된 안읽음 개수와 Redis에 아직 남아있는 안읽음 개수를 합산한다.
     * 메시지가 Redis에 먼저 저장되기 때문에 둘을 합쳐야 관리자 목록의 안읽음 개수가 정확하다.
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

    /**
     * 관리자 목록 페이징 계산을 위한 전체 상담방 개수 조회
     */
    @Override
    @Transactional(readOnly = true)
    public int countAdminRooms(String status, String category, String keyword) {
        return chatDao.countAdminRooms(status, category, keyword);
    }

    /**
     * 관리자 목록의 마지막 메시지와 마지막 메시지 시간을 갱신한다.
     */
    @Override
    public void updateLastMessage(Integer roomNo, String lastMessage) {
        if (roomNo == null) {
            return;
        }

        ChatRoomDto chatRoomDto = new ChatRoomDto();
        chatRoomDto.setRoomNo(roomNo);
        chatRoomDto.setLastMessage(lastMessage);

        chatDao.updateLastMessage(chatRoomDto);
    }

    /**
     * 담당 관리자가 없는 상담방에 관리자를 배정한다.
     */
    @Override
    public void assignAdmin(Integer roomNo, Integer adminNo) {
        if (roomNo == null || adminNo == null) {
            return;
        }

        chatDao.assignAdmin(roomNo, adminNo);
    }

    /**
     * 사용자 문의 유형 변경
     *
     * 상담방이 OPEN 상태이고, 요청한 사용자가 해당 상담방의 주인일 때만 category를 변경한다.
     */
    @Override
    public void changeCategory(Integer roomNo, Integer userNo, String category) {
        category = normalizeCategory(category);

        ChatRoomDto chatRoom = chatDao.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return;
        }

        if (!"OPEN".equals(chatRoom.getStatus())) {
            return;
        }

        if (chatRoom.getUserNo() == null || !chatRoom.getUserNo().equals(userNo)) {
            return;
        }

        chatDao.updateRoomCategory(roomNo, category);
    }

    /**
     * 문의 유형 값 정리
     *
     * null, 빈 문자열, 허용되지 않은 값이 들어오면 ETC로 처리한다.
     * DB CHECK 제약조건 오류를 사전에 방지하기 위한 방어 코드이다.
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "ETC";
        }

        if (!isValidCategory(category)) {
            return "ETC";
        }

        return category;
    }

    /**
     * 사용자 본인의 상담내역 목록 조회
     *
     * 보안 기준:
     * - userNo는 Controller에서 로그인 사용자 번호로 넘긴다.
     * - SQL에서도 WHERE r.user_no = #{userNo} 조건으로 본인 상담방만 조회한다.
     *
     * unreadCount:
     * - 관리자가 보냈고 사용자가 아직 읽지 않은 메시지 개수이다.
     * - Oracle DB에 저장된 메시지와 Redis에 남아있는 메시지 개수를 합산한다.
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

    //사용자 본인의 상담내역 전체 개수 조회
    @Override
    @Transactional(readOnly = true)
    public int countUserRooms(Integer userNo) {
        if (userNo == null) {
            return 0;
        }
        return chatDao.countUserRooms(userNo);
    }

    private boolean isValidCategory(String category) {
        return List.of("MAIL", "MAP", "STOCK", "NEWS", "WEATHER", "CALENDAR", "ETC")
                .contains(category);
    }
}