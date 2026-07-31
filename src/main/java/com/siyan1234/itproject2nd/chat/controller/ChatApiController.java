package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.service.ChatAccessService;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.support.ChatMessagePolicy;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 사용자 채팅 API Controller입니다.
 *
 * 화면 이동은 ChatController가 담당하고,
 * 메시지 목록 조회와 테스트용 HTTP 메시지 저장 API만 이 Controller에서 담당합니다.
 */
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatApiController {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatAccessService chatAccessService;

    /** 채팅방 메시지 목록 조회 API */
    @GetMapping("/{roomNo}/messages")
    public List<ChatMessageDto> messages(
            @PathVariable Integer roomNo,
            HttpSession session
    ) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return List.of();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!chatAccessService.canAccessRoom(loginUser, chatRoom)) {
            return List.of();
        }

        // ChatService 내부에서 Oracle과 Redis를 함께 읽음 처리합니다.
        chatService.updateReadYn(roomNo, loginUser.getNo());

        List<ChatMessageDto> messages = new ArrayList<>();
        messages.addAll(chatService.findMessagesByRoomNo(roomNo));
        messages.addAll(chatRedisService.findMessages(roomNo));
        messages.sort(Comparator.comparing(
                ChatMessageDto::getCreatedDate,
                Comparator.nullsLast(Comparator.naturalOrder())
        ));

        return messages;
    }

    /** 초기 테스트용 HTTP 메시지 저장 API. 실제 실시간 메시지는 WebSocket이 담당합니다. */
    @PostMapping("/message")
    public ResponseEntity<String> sendMessage(
            @RequestBody ChatMessageDto chatMessageDto,
            HttpSession session
    ) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("login-required");
        }

        if (chatMessageDto == null || chatMessageDto.getRoomNo() == null) {
            return ResponseEntity.badRequest().body("invalid-message");
        }

        String messageContent = ChatMessagePolicy.normalize(chatMessageDto.getMessageContent());
        if (ChatMessagePolicy.isBlank(messageContent)) {
            return ResponseEntity.badRequest().body("message-required");
        }
        if (ChatMessagePolicy.exceedsMaxLength(messageContent)) {
            return ResponseEntity.badRequest().body("message-too-long");
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(chatMessageDto.getRoomNo());

        if (!chatAccessService.canAccessRoom(loginUser, chatRoom)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("forbidden");
        }

        if (!ChatRoomStatus.isOpen(chatRoom.getStatus())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("closed");
        }

        chatMessageDto.setSenderNo(loginUser.getNo());
        chatMessageDto.setMessageContent(messageContent);
        chatService.saveMessage(chatMessageDto);
        return ResponseEntity.ok("ok");
    }
}
