package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public String chatPage(HttpSession session, Model model) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/user-login";
        }

        ChatRoomDto chatRoom = chatService.getOrCreateRoom(loginUser.getNo());

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/chat";
    }

    @GetMapping("/admin")
    public String adminChatList(HttpSession session, Model model) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        if (!"ADMIN".equals(loginUser.getRole())) {
            return "redirect:/";
        }

        List<ChatRoomDto> roomList = chatService.findAllRooms();

        model.addAttribute("roomList", roomList);
        model.addAttribute("loginUser", loginUser);

        return "chat/admin-chat-list";
    }

    @GetMapping("/admin/{roomNo}")
    public String adminChatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/member/login";
        }

        if (!"ADMIN".equals(loginUser.getRole())) {
            return "redirect:/";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/admin-chat-room";
    }

    @ResponseBody
    @GetMapping("/{roomNo}/messages")
    public List<ChatMessageDto> messages(@PathVariable Integer roomNo) {
        return chatService.findMessagesByRoomNo(roomNo);
    }

    @ResponseBody
    @PostMapping("/message")
    public String sendMessage(@RequestBody ChatMessageDto chatMessageDto) {
        chatService.saveMessage(chatMessageDto);
        return "ok";
    }

    @PostMapping("/{roomNo}/close")
    public String closeRoom(@PathVariable Integer roomNo) {
        chatService.closeRoom(roomNo);
        return "redirect:/chat/admin";
    }
}