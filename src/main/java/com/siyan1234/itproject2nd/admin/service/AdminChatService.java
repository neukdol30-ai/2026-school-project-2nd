package com.siyan1234.itproject2nd.admin.service;

import com.siyan1234.itproject2nd.admin.dao.AdminChatDao;
import com.siyan1234.itproject2nd.admin.dto.AdminChatRoomListResponseDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.support.ChatMessageFactory;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.websocket.ChatWebSocketBroadcaster;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자 콘솔 상담 관리 Service입니다. */
@Service
@RequiredArgsConstructor
public class AdminChatService {

    private final AdminChatDao adminChatDao;
    private final AdminDashboardService adminDashboardService;
    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatWebSocketBroadcaster chatWebSocketBroadcaster;

    @Transactional(readOnly = true)
    public List<RecentChatRoomDto> findRooms(
            String status,
            String category,
            String keyword,
            Integer viewerNo,
            int page,
            int size
    ) {
        int offset = AdminPagingHelper.calculateOffset(page, size);
        List<RecentChatRoomDto> roomList = adminChatDao.findAdminChatRooms(
                AdminPagingHelper.cleanText(status),
                AdminPagingHelper.cleanText(category),
                AdminPagingHelper.cleanText(keyword),
                offset,
                size
        );

        if (viewerNo == null) {
            return roomList;
        }

        for (RecentChatRoomDto room : roomList) {
            long dbUnreadCount = room.getUnreadCount() == null ? 0L : room.getUnreadCount();
            int redisUnreadCount = chatRedisService.countUnreadMessages(room.getRoomNo(), viewerNo);
            room.setUnreadCount(dbUnreadCount + redisUnreadCount);
        }

        return roomList;
    }

    @Transactional(readOnly = true)
    public long countRooms(String status, String category, String keyword) {
        Long count = adminChatDao.countAdminChatRooms(
                AdminPagingHelper.cleanText(status),
                AdminPagingHelper.cleanText(category),
                AdminPagingHelper.cleanText(keyword)
        );
        return count == null ? 0L : count;
    }

    @Transactional(readOnly = true)
    public AdminChatRoomListResponseDto createRoomListResponse(
            String status,
            String category,
            String keyword,
            MemberDto loginAdmin,
            int page,
            int size
    ) {
        String cleanStatus = AdminPagingHelper.cleanText(status);
        String cleanCategory = AdminPagingHelper.cleanText(category);
        String cleanKeyword = AdminPagingHelper.cleanText(keyword);

        long totalCount = countRooms(cleanStatus, cleanCategory, cleanKeyword);
        int totalPages = AdminPagingHelper.calculateTotalPages(totalCount, size);
        int safePage = Math.min(AdminPagingHelper.normalizePage(page), totalPages);

        List<RecentChatRoomDto> roomList = findRooms(
                cleanStatus,
                cleanCategory,
                cleanKeyword,
                loginAdmin == null ? null : loginAdmin.getNo(),
                safePage,
                size
        );
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        AdminChatRoomListResponseDto response = new AdminChatRoomListResponseDto();
        response.setSuccess(true);
        response.setRoomList(roomList);
        response.setChatStatus(cleanStatus);
        response.setChatCategory(cleanCategory);
        response.setChatKeyword(cleanKeyword);
        response.setChatPage(safePage);
        response.setChatSize(size);
        response.setChatTotalCount(totalCount);
        response.setChatTotalPages(totalPages);
        response.setDashboard(dashboard);
        return response;
    }

    @Transactional
    public ChatRoomDto assignAdminIfEmpty(Integer roomNo, Integer adminNo) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return null;
        }

        if (chatRoom.getAdminNo() == null) {
            chatService.assignAdmin(roomNo, adminNo);
            chatRoom = chatService.findRoomByRoomNo(roomNo);
            chatWebSocketBroadcaster.broadcastAdminListRefresh(roomNo);
        }

        return chatRoom;
    }

    @Transactional
    public boolean closeRoom(Integer roomNo, Integer adminNo) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null || ChatRoomStatus.isClosed(chatRoom.getStatus())) {
            return false;
        }

        chatService.closeRoom(roomNo);
        ChatMessageDto closeMessage = ChatMessageFactory.closeMessage(roomNo, adminNo);

        chatRedisService.saveMessage(closeMessage);
        chatService.updateLastMessage(roomNo, closeMessage.getMessageContent());
        chatWebSocketBroadcaster.broadcastClose(roomNo, closeMessage);
        chatWebSocketBroadcaster.broadcastAdminListRefresh(roomNo);
        return true;
    }

    @Transactional
    public boolean deleteClosedRoom(Integer roomNo) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null || !ChatRoomStatus.isClosed(chatRoom.getStatus())) {
            return false;
        }

        chatRedisService.deleteMessages(roomNo);
        chatService.deleteRoom(roomNo);
        chatWebSocketBroadcaster.broadcastAdminListRefresh(roomNo);
        return true;
    }

    @Transactional
    public AdminDeleteResultDto deleteClosedRooms(List<Integer> roomNoList) {
        if (roomNoList == null || roomNoList.isEmpty()) {
            return new AdminDeleteResultDto(0, 0, 0);
        }

        int deletedCount = 0;

        for (Integer roomNo : roomNoList) {
            if (deleteClosedRoom(roomNo)) {
                deletedCount++;
            }
        }

        return new AdminDeleteResultDto(roomNoList.size(), deletedCount, roomNoList.size() - deletedCount);
    }

    @Transactional(readOnly = true)
    public ChatRoomDto findRoomByRoomNo(Integer roomNo) {
        return chatService.findRoomByRoomNo(roomNo);
    }

}