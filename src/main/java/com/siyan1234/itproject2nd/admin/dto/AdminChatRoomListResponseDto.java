package com.siyan1234.itproject2nd.admin.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** /admin?view=chats 실시간 갱신 JSON 응답 DTO입니다. */
@Getter
@Setter
public class AdminChatRoomListResponseDto {

    private boolean success;
    private String message;
    private List<RecentChatRoomDto> roomList = new ArrayList<>();

    private String chatStatus;
    private String chatCategory;
    private String chatKeyword;

    private int chatPage;
    private int chatSize;
    private long chatTotalCount;
    private int chatTotalPages;

    private AdminDashboardDto dashboard;

    public static AdminChatRoomListResponseDto fail(String message, int chatSize) {
        AdminChatRoomListResponseDto response = new AdminChatRoomListResponseDto();
        response.setSuccess(false);
        response.setMessage(message);
        response.setChatPage(1);
        response.setChatSize(chatSize);
        response.setChatTotalCount(0L);
        response.setChatTotalPages(1);
        return response;
    }
}
