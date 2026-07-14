package com.siyan1234.itproject2nd.admin.controller;

import com.siyan1234.itproject2nd.admin.dto.AdminChatRoomListResponseDto;
import com.siyan1234.itproject2nd.admin.dto.AdminDeleteResultDto;
import com.siyan1234.itproject2nd.admin.service.AdminChatService;
import com.siyan1234.itproject2nd.admin.support.AdminPagingHelper;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
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
        return "redirect:/admin?view=chats";
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
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        MemberDto loginAdmin = loginMemberResolver.fromPrincipal(customUserDetails);

        if (!loginMemberResolver.isAdmin(loginAdmin)) {
            return "redirect:/admin/login";
        }

        ChatRoomDto chatRoom = adminChatService.assignAdminIfEmpty(roomNo, loginAdmin.getNo());

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
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

        ChatRoomDto chatRoom = adminChatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if (!adminChatService.closeRoom(roomNo, loginAdmin.getNo())) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "이미 종료된 상담입니다.");
            return "redirect:/admin/chats/" + roomNo;
        }

        redirectAttributes.addFlashAttribute("adminMessage", "상담방 #" + roomNo + "번을 종료했습니다.");
        return "redirect:/admin/chats/" + roomNo;
    }

    @PostMapping("/{roomNo}/delete")
    public String deleteChatRoom(
            @PathVariable("roomNo") Integer roomNo,
            RedirectAttributes redirectAttributes
    ) {
        ChatRoomDto chatRoom = adminChatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "존재하지 않는 상담방입니다.");
            return "redirect:/admin?view=chats";
        }

        if (!adminChatService.deleteClosedRoom(roomNo)) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "진행 중 상담은 먼저 종료한 뒤 삭제할 수 있습니다.");
            return "redirect:/admin?view=chats";
        }

        redirectAttributes.addFlashAttribute("adminMessage", "종료된 상담방 #" + roomNo + "번을 삭제했습니다.");
        return "redirect:/admin?view=chats";
    }

    @PostMapping("/delete")
    public String deleteSelectedChatRooms(
            @RequestParam(value = "roomNoList", required = false) List<Integer> roomNoList,
            RedirectAttributes redirectAttributes
    ) {
        AdminDeleteResultDto result = adminChatService.deleteClosedRooms(roomNoList);

        if (result.getRequestedCount() == 0) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제할 상담방을 선택해 주세요.");
            return "redirect:/admin?view=chats";
        }

        if (!result.hasDeletedItem()) {
            redirectAttributes.addFlashAttribute("adminErrorMessage", "삭제 가능한 종료 상담이 없습니다. 진행 중 상담은 종료 후 삭제해 주세요.");
        } else {
            redirectAttributes.addFlashAttribute("adminMessage", "종료 상담 " + result.getDeletedCount() + "건을 삭제했습니다."
                    + (result.getSkippedCount() > 0 ? " 진행 중이거나 없는 상담 " + result.getSkippedCount() + "건은 제외했습니다." : ""));
        }

        return "redirect:/admin?view=chats";
    }
}
