package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminChatRoomListResponseDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminChatService;
import com.siyan1234.itproject2nd.admin.support.AdminFlashMessage;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.admin.support.AdminRoutes;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 콘솔 상담 관리 Controller입니다.
 *
 * /chat 영역은 사용자 채팅 전용으로 유지하고,
 * 관리자 운영 액션은 /admin/chats 아래로 모았습니다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/chats")
public class AdminChatController {

    private static final int DEFAULT_CHAT_SIZE = 10;

    private final AdminChatService adminChatService;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping
    public String adminChats() {
        return AdminRoutes.ADMIN_CHATS;
    }

    @ResponseBody
    @GetMapping("/rooms")
    public AdminChatRoomListResponseDto adminChatRoomsForConsole(
            @RequestParam(value = "chatStatus", required = false) String chatStatus,
            @RequestParam(value = "chatCategory", required = false) String chatCategory,
            @RequestParam(value = "chatKeyword", required = false) String chatKeyword,
            @RequestParam(value = "chatPage", defaultValue = "1") int chatPage,
            @RequestParam(value = "chatSize", defaultValue = "10") int chatSize,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        int safeChatSize = AdminPagingHelper.normalizeSize(chatSize, DEFAULT_CHAT_SIZE);

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            return AdminChatRoomListResponseDto.fail("관리자 권한이 필요합니다.", safeChatSize);
        }

        return adminChatService.createRoomListResponse(
                chatStatus,
                chatCategory,
                chatKeyword,
                loginAdmin,
                AdminPagingHelper.normalizePage(chatPage),
                safeChatSize
        );
    }

    @GetMapping("/{roomNo}")
    public String adminChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            RedirectAttributes redirectAttributes
    ) {
        redirectAttributes.addAttribute("view", "chatRoom");
        redirectAttributes.addAttribute("roomNo", roomNo);
        return AdminRoutes.ADMIN_HOME;
    }

    @PostMapping("/{roomNo}/close")
    public String closeChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            return AdminRoutes.ADMIN_LOGIN;
        }

        ChatRoomDto chatRoom = adminChatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_ROOM_NOT_FOUND);
            return AdminRoutes.ADMIN_CHATS;
        }

        if (!adminChatService.closeRoom(roomNo, loginAdmin.getNo())) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_ALREADY_CLOSED);
            return AdminRoutes.chatRoom(roomNo);
        }

        redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.chatClosed(roomNo));
        return AdminRoutes.chatRoom(roomNo);
    }

    @PostMapping("/{roomNo}/delete")
    public String deleteChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            RedirectAttributes redirectAttributes
    ) {
        ChatRoomDto chatRoom = adminChatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_ROOM_NOT_FOUND);
            return AdminRoutes.ADMIN_CHATS;
        }

        if (!adminChatService.deleteClosedRoom(roomNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_DELETE_OPEN_DENIED);
            return AdminRoutes.ADMIN_CHATS;
        }

        redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.chatDeleted(roomNo));
        return AdminRoutes.ADMIN_CHATS;
    }

    @PostMapping("/delete")
    public String deleteSelectedChatRooms(
            @RequestParam(value = "roomNoList", required = false) List<Integer> roomNoList,
            RedirectAttributes redirectAttributes
    ) {
        AdminDeleteResultDto result = adminChatService.deleteClosedRooms(roomNoList);

        if (result.getRequestedCount() == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_DELETE_NOT_SELECTED);
            return AdminRoutes.ADMIN_CHATS;
        }

        if (!result.hasDeletedItem()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", AdminFlashMessage.CHAT_DELETE_NO_AVAILABLE_ROOM);
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", AdminFlashMessage.selectedChatsDeleted(result));
        }

        return AdminRoutes.ADMIN_CHATS;
    }
}
