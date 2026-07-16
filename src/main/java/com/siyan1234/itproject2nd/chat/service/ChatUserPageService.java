package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.kakao.service.KakaoNotifyService;
import com.siyan1234.itproject2nd.chat.support.ChatRoomStatus;
import com.siyan1234.itproject2nd.chat.websocket.ChatWebSocketBroadcaster;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import java.util.List;

/**
 * 사용자 채팅 화면에 필요한 Model 조립과 화면 이동 결정을 담당합니다.
 *
 * ChatController가 URL 매핑만 담당하도록 하기 위해 화면 처리 로직을 분리했습니다.
 */
@Service
@RequiredArgsConstructor
public class ChatUserPageService {

    private static final int HISTORY_PAGE_SIZE = 10;

    private final ChatService chatService;
    private final ChatAccessService chatAccessService;
    private final ChatWebSocketBroadcaster chatWebSocketBroadcaster;
    private final KakaoNotifyService kakaoNotifyService;

    public String showChatHome(HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto openRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("openRoom", openRoom);
        model.addAttribute("isMobile", mobile);

        return mobile ? "chat/mobile/chat-home" : "chat/pc/chat-home";
    }

    public String continueChatView(
            String personalizationCookie,
            String lastChatView,
            HttpSession session
    ) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        boolean personalizationAllowed = "Y".equalsIgnoreCase(personalizationCookie)
                || "true".equalsIgnoreCase(personalizationCookie);

        if (personalizationAllowed && "mobile".equalsIgnoreCase(lastChatView)) {
            return "redirect:/chat/mobile";
        }

        return "redirect:/chat";
    }

    public String startChat(String category, HttpSession session, boolean mobile) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto beforeRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());
        ChatRoomDto chatRoom = chatService.getOrCreateRoom(loginUser.getNo(), category);

        if (beforeRoom == null) {
            kakaoNotifyService.sendNewChatRoomAlert(chatRoom);
        }

        chatWebSocketBroadcaster.broadcastAdminListRefresh(chatRoom.getRoomNo());

        return mobile
                ? "redirect:/chat/mobile/room/" + chatRoom.getRoomNo()
                : "redirect:/chat/room/" + chatRoom.getRoomNo();
    }

    public String showChatRoom(Integer roomNo, HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (!chatAccessService.canAccessRoom(loginUser, chatRoom)) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("isMobile", mobile);

        return mobile ? "chat/mobile/chat-room" : "chat/pc/chat-room";
    }

    public String showUserHistory(int page, HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        List<ChatRoomDto> roomList = chatService.findUserRooms(loginUser.getNo(), page, HISTORY_PAGE_SIZE);
        int totalCount = chatService.countUserRooms(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("roomList", roomList);
        model.addAttribute("page", page);
        model.addAttribute("size", HISTORY_PAGE_SIZE);
        model.addAttribute("totalCount", totalCount);

        return mobile ? "chat/mobile/chat-history" : "chat/pc/chat-history";
    }

    public String changeCategory(Integer roomNo, String category, HttpSession session, boolean mobile) {
        MemberDto loginUser = chatAccessService.getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        if (!ChatRoomStatus.isOpen(chatRoom.getStatus())) {
            return mobile ? "redirect:/chat/mobile/room/" + roomNo : "redirect:/chat/room/" + roomNo;
        }

        if (!chatAccessService.isOwnerOfOpenRoom(loginUser, chatRoom)) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        chatService.changeCategory(roomNo, loginUser.getNo(), category);
        chatWebSocketBroadcaster.broadcastAdminListRefresh(roomNo);

        return mobile ? "redirect:/chat/mobile/room/" + roomNo : "redirect:/chat/room/" + roomNo;
    }

    private String redirectToUserLogin() {
        return "redirect:/member/login";
    }
}
