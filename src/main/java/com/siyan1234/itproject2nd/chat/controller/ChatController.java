package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.websocket.ChatHandler;
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
    private final ChatRedisService chatRedisService;
    private final ChatHandler chatHandler;

    @GetMapping
    public String chatHome(HttpSession session, Model model) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/user-login";
        }
        ChatRoomDto openRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("openRoom", openRoom);

        return "chat/chat-home";
    }

    @PostMapping("/start")
    public String startChat(
            @RequestParam String category,
            HttpSession session
    ){
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/user-login";
        }
        ChatRoomDto chatRoom = chatService.getOrCreateRoom(loginUser.getNo(),  category);
        return "redirect:/chat/room/" + chatRoom.getRoomNo();
    }

    @GetMapping("/room/{roomNo}")
    public String chatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/user-login";
        }
        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/chat";
    }

    @GetMapping("/admin")
    public String adminChatList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/admin-login";
        }

        if (!"ADMIN".equals(loginUser.getRole())) {
            return "redirect:/";
        }

        if (page < 1) {
            page = 1;
        }

        if (size < 1) {
            size = 10;
        }

        int totalCount = chatService.countAdminRooms(status, category, keyword);
        int totalPage = (int) Math.ceil((double) totalCount / size);

        if (totalPage < 1) {
            totalPage = 1;
        }

        if (page > totalPage) {
            page = totalPage;
        }

        List<ChatRoomDto> roomList = chatService.findAdminRooms(
                status,
                category,
                keyword,
                loginUser.getNo(),
                page,
                size
        );

        int pageBlockSize = 5;

        int startPageNo = ((page - 1) / pageBlockSize) * pageBlockSize + 1;
        int endPageNo = Math.min(startPageNo + pageBlockSize - 1, totalPage);

        model.addAttribute("roomList", roomList);
        model.addAttribute("loginUser", loginUser);

        model.addAttribute("status", status);
        model.addAttribute("category", category);
        model.addAttribute("keyword", keyword);

        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalCount", totalCount);
        model.addAttribute("totalPage", totalPage);

        model.addAttribute("startPageNo", startPageNo);
        model.addAttribute("endPageNo", endPageNo);

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
            return "redirect:/temp/admin-login";
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
    public List<ChatMessageDto> messages
            (@PathVariable Integer roomNo,
             HttpSession session
    ) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser != null){
            chatService.updateReadYn(roomNo, loginUser.getNo());
            chatRedisService.updateReadYn(roomNo, loginUser.getNo());
        }

        List<ChatMessageDto> dbMessages = chatService.findMessagesByRoomNo(roomNo);
        List<ChatMessageDto> redisMessages = chatRedisService.findMessages(roomNo);

        dbMessages.addAll(redisMessages);

        return dbMessages;
    }

    @ResponseBody
    @PostMapping("/message")
    public String sendMessage(@RequestBody ChatMessageDto chatMessageDto) {
        chatService.saveMessage(chatMessageDto);
        return "ok";
    }

    @PostMapping("/{roomNo}/close")
    public String closeRoom
            (@PathVariable Integer roomNo,
             HttpSession session
    ) {
        MemberDto loginUser = (MemberDto) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/temp/admin-login";
        }

        chatService.closeRoom(roomNo);

        ChatMessageDto closeMessage = new ChatMessageDto();
        closeMessage.setRoomNo(roomNo);
        closeMessage.setSenderNo(loginUser.getNo());
        closeMessage.setMessageContent("상담이 종료되었습니다.");
        closeMessage.setReadYn("N");
        closeMessage.setCreatedDate(java.time.LocalDateTime.now());

        chatRedisService.saveMessage(closeMessage);

        chatHandler.broadcastClose(roomNo, closeMessage);

        return "redirect:/chat/admin";
    }
}