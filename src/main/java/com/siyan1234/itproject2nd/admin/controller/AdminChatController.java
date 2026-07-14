package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminDashboardDto;
import com.siyan1234.itproject2nd.admin.dto.RecentChatRoomDto;
import com.siyan1234.itproject2nd.admin.service.AdminDashboardService;
import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.websocket.ChatHandler;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private final AdminDashboardService adminDashboardService;
    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatHandler chatHandler;
    private final LoginMemberResolver loginMemberResolver;

    @GetMapping
    public String adminChats() {
        return "redirect:/admin?view=chats";
    }

    @ResponseBody
    @GetMapping("/rooms")
    public Map<String, Object> adminChatRoomsForConsole(
            @RequestParam(value = "chatStatus", required = false) String chatStatus,
            @RequestParam(value = "chatCategory", required = false) String chatCategory,
            @RequestParam(value = "chatKeyword", required = false) String chatKeyword,
            @RequestParam(value = "chatPage", defaultValue = "1") int chatPage,
            @RequestParam(value = "chatSize", defaultValue = "10") int chatSize,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);
        Map<String, Object> result = new HashMap<>();

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            result.put("success", false);
            result.put("message", "관리자 권한이 필요합니다.");
            result.put("roomList", List.of());
            result.put("chatPage", 1);
            result.put("chatSize", chatSize);
            result.put("chatTotalCount", 0);
            result.put("chatTotalPages", 1);
            return result;
        }

        chatPage = normalizePage(chatPage);
        chatSize = normalizeSize(chatSize);

        String cleanChatStatus = cleanText(chatStatus);
        String cleanChatCategory = cleanText(chatCategory);
        String cleanChatKeyword = cleanText(chatKeyword);

        long chatTotalCount = adminDashboardService.countAdminChatRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword
        );
        int chatTotalPages = calculateTotalPages(chatTotalCount, chatSize);

        if (chatPage > chatTotalPages) {
            chatPage = chatTotalPages;
        }

        List<RecentChatRoomDto> roomList = adminDashboardService.findAdminChatRooms(
                cleanChatStatus,
                cleanChatCategory,
                cleanChatKeyword,
                loginAdmin.getNo(),
                chatPage,
                chatSize
        );
        AdminDashboardDto dashboard = adminDashboardService.getDashboard();

        result.put("success", true);
        result.put("roomList", roomList);
        result.put("chatStatus", cleanChatStatus);
        result.put("chatCategory", cleanChatCategory);
        result.put("chatKeyword", cleanChatKeyword);
        result.put("chatPage", chatPage);
        result.put("chatSize", chatSize);
        result.put("chatTotalCount", chatTotalCount);
        result.put("chatTotalPages", chatTotalPages);
        result.put("dashboard", dashboard);

        return result;
    }

    @GetMapping("/{roomNo}")
    public String adminChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            return "redirect:/admin/login";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if (chatRoom.getAdminNo() == null) {
            chatService.assignAdmin(roomNo, loginAdmin.getNo());
            chatRoom = chatService.findRoomByRoomNo(roomNo);
            chatHandler.broadcastAdminListRefresh(roomNo);
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginAdmin);

        return "chat/admin/admin-chat-room";
    }

    @PostMapping("/{roomNo}/close")
    public String closeChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            return "redirect:/admin/login";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if ("CLOSED".equals(chatRoom.getStatus())) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "이미 종료된 상담입니다.");
            return "redirect:/admin/chats/" + roomNo;
        }

        chatService.closeRoom(roomNo);
        ChatMessageDto closeMessage = createCloseMessage(roomNo, loginAdmin.getNo());

        chatRedisService.saveMessage(closeMessage);
        chatService.updateLastMessage(roomNo, closeMessage.getMessageContent());
        chatHandler.broadcastClose(roomNo, closeMessage);
        chatHandler.broadcastAdminListRefresh(roomNo);

        redirectAttributes.addFlashAttribute("adminMessage", "상담방 #" + roomNo + "번을 종료했습니다.");
        return "redirect:/admin/chats/" + roomNo;
    }

    @PostMapping("/{roomNo}/delete")
    public String deleteChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            RedirectAttributes redirectAttributes
    ) {
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if (!"CLOSED".equals(chatRoom.getStatus())) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "진행 중 상담은 먼저 종료한 뒤 삭제할 수 있습니다.");
            return "redirect:/admin?view=chats";
        }

        deleteClosedRoom(roomNo);
        redirectAttributes.addFlashAttribute("adminMessage", "종료된 상담방 #" + roomNo + "번을 삭제했습니다.");
        return "redirect:/admin?view=chats";
    }

    @PostMapping("/delete")
    public String deleteSelectedChatRooms(
            @RequestParam(value = "roomNoList", required = false) List<Integer> roomNoList,
            RedirectAttributes redirectAttributes
    ) {
        if (roomNoList == null || roomNoList.isEmpty()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 상담방을 선택해 주세요.");
            return "redirect:/admin?view=chats";
        }

        int deletedCount = 0;
        int skippedCount = 0;

        for (Integer roomNo : roomNoList) {
            ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

            if (chatRoom == null || !"CLOSED".equals(chatRoom.getStatus())) {
                skippedCount++;
                continue;
            }

            deleteClosedRoom(roomNo);
            deletedCount++;
        }

        if (deletedCount == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제 가능한 종료 상담이 없습니다. 진행 중 상담은 종료 후 삭제해 주세요.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "종료 상담 " + deletedCount + "건을 삭제했습니다."
                    + (skippedCount > 0 ? " 진행 중이거나 없는 상담 " + skippedCount + "건은 제외했습니다." : ""));
        }

        return "redirect:/admin?view=chats";
    }

    private ChatMessageDto createCloseMessage(Integer roomNo, Integer adminNo) {
        ChatMessageDto closeMessage = new ChatMessageDto();
        closeMessage.setRoomNo(roomNo);
        closeMessage.setSenderNo(adminNo);
        closeMessage.setMessageContent("상담이 종료되었습니다.");
        closeMessage.setReadYn("N");
        closeMessage.setCreatedDate(LocalDateTime.now());
        return closeMessage;
    }

    private void deleteClosedRoom(Integer roomNo) {
        chatRedisService.deleteMessages(roomNo);
        chatService.deleteRoom(roomNo);
        chatHandler.broadcastAdminListRefresh(roomNo);
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_CHAT_SIZE;
        }

        return Math.min(size, 50);
    }

    private int calculateTotalPages(long totalCount, int size) {
        if (totalCount <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) totalCount / size);
    }

    private String cleanText(String value) {
        if (value == null) {
            return "";
        }

        return value.trim();
    }
}
