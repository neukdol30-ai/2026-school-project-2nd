package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.kakao.service.KakaoNotifyService;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.websocket.ChatHandler;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 사용자 1:1 채팅 Controller입니다.
 *
 * 리팩토링 기준:
 * - /chat 영역은 사용자 상담 홈/상담방/상담내역만 담당합니다.
 * - 관리자 상담 운영은 /admin/chats 영역으로 분리했습니다.
 * - 기존 /chat/admin URL은 호환용 redirect만 유지합니다.
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatHandler chatHandler;
    private final KakaoNotifyService kakaoNotifyService;
    private final LoginMemberResolver loginMemberResolver;

    /** 사용자 PC 상담 홈 */
    @GetMapping
    public String chatHome(HttpSession session, Model model) {
        return showChatHome(session, model, false);
    }

    /** 사용자 모바일 상담 홈 */
    @GetMapping("/mobile")
    public String mobileChatHome(HttpSession session, Model model) {
        return showChatHome(session, model, true);
    }

    /** 최근 사용한 상담 화면으로 이동 */
    @GetMapping("/continue")
    public String continueChatView(
            @CookieValue(value = "SECONDPRO_PERSONALIZATION", required = false) String personalizationCookie,
            @CookieValue(value = "SECONDPRO_LAST_CHAT_VIEW", required = false) String lastChatView,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

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

    /** 사용자 PC 상담 시작 */
    @PostMapping("/start")
    public String startChat(
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return startChatByView(category, session, false);
    }

    /** 사용자 모바일 상담 시작 */
    @PostMapping("/mobile/start")
    public String startMobileChat(
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return startChatByView(category, session, true);
    }

    /** 사용자 PC 상담방 */
    @GetMapping("/room/{roomNo}")
    public String chatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        return showChatRoom(roomNo, session, model, false);
    }

    /** 사용자 모바일 상담방 */
    @GetMapping("/mobile/room/{roomNo}")
    public String mobileChatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        return showChatRoom(roomNo, session, model, true);
    }

    /** 사용자 PC 상담내역 */
    @GetMapping("/history")
    public String userHistory(
            @RequestParam(defaultValue = "1") int page,
            HttpSession session,
            Model model
    ) {
        return showUserHistory(page, session, model, false);
    }

    /** 사용자 모바일 상담내역 */
    @GetMapping("/mobile/history")
    public String userMobileHistory(
            @RequestParam(defaultValue = "1") int page,
            HttpSession session,
            Model model
    ) {
        return showUserHistory(page, session, model, true);
    }

    /** 기존 관리자 상담 목록 URL 호환용 redirect */
    @GetMapping("/admin")
    public String legacyAdminChatList() {
        return "redirect:/admin?view=chats";
    }

    /** 기존 관리자 상담 상세 URL 호환용 redirect */
    @GetMapping("/admin/{roomNo}")
    public String legacyAdminChatRoom(@PathVariable Integer roomNo) {
        return "redirect:/admin/chats/" + roomNo;
    }

    /** 채팅방 메시지 목록 조회 API */
    @ResponseBody
    @GetMapping("/{roomNo}/messages")
    public List<ChatMessageDto> messages(
            @PathVariable Integer roomNo,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return List.of();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null || !canAccessRoom(loginUser, chatRoom)) {
            return List.of();
        }

        chatService.updateReadYn(roomNo, loginUser.getNo());
        chatRedisService.updateReadYn(roomNo, loginUser.getNo());

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
    @ResponseBody
    @PostMapping("/message")
    public String sendMessage(
            @RequestBody ChatMessageDto chatMessageDto,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return "login-required";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(chatMessageDto.getRoomNo());

        if (chatRoom == null || !canAccessRoom(loginUser, chatRoom)) {
            return "forbidden";
        }

        if (!"OPEN".equals(chatRoom.getStatus())) {
            return "closed";
        }

        chatMessageDto.setSenderNo(loginUser.getNo());
        chatService.saveMessage(chatMessageDto);
        return "ok";
    }

    /** 사용자 PC 문의 유형 변경 */
    @PostMapping("/{roomNo}/category")
    public String changeCategory(
            @PathVariable Integer roomNo,
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return changeCategoryByView(roomNo, category, session, false);
    }

    /** 사용자 모바일 문의 유형 변경 */
    @PostMapping("/mobile/{roomNo}/category")
    public String changeMobileCategory(
            @PathVariable Integer roomNo,
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return changeCategoryByView(roomNo, category, session, true);
    }

    private String showChatHome(HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto openRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("openRoom", openRoom);
        model.addAttribute("isMobile", mobile);

        return mobile ? "chat/mobile/chat-home" : "chat/pc/chat-home";
    }

    private String startChatByView(String category, HttpSession session, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto beforeRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());
        ChatRoomDto chatRoom = chatService.getOrCreateRoom(loginUser.getNo(), category);

        if (beforeRoom == null) {
            kakaoNotifyService.sendNewChatRoomAlert(chatRoom);
        }

        chatHandler.broadcastAdminListRefresh(chatRoom.getRoomNo());

        return mobile
                ? "redirect:/chat/mobile/room/" + chatRoom.getRoomNo()
                : "redirect:/chat/room/" + chatRoom.getRoomNo();
    }

    private String showChatRoom(Integer roomNo, HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null || !canAccessRoom(loginUser, chatRoom)) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("isMobile", mobile);

        return mobile ? "chat/mobile/chat-room" : "chat/pc/chat-room";
    }

    private String showUserHistory(int page, HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        int size = 10;
        List<ChatRoomDto> roomList = chatService.findUserRooms(loginUser.getNo(), page, size);
        int totalCount = chatService.countUserRooms(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("roomList", roomList);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalCount", totalCount);

        return mobile ? "chat/mobile/chat-history" : "chat/pc/chat-history";
    }

    private String changeCategoryByView(Integer roomNo, String category, HttpSession session, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        if (!"OPEN".equals(chatRoom.getStatus())) {
            return mobile ? "redirect:/chat/mobile/room/" + roomNo : "redirect:/chat/room/" + roomNo;
        }

        if (chatRoom.getUserNo() == null || !chatRoom.getUserNo().equals(loginUser.getNo())) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        chatService.changeCategory(roomNo, loginUser.getNo(), category);
        chatHandler.broadcastAdminListRefresh(roomNo);

        return mobile ? "redirect:/chat/mobile/room/" + roomNo : "redirect:/chat/room/" + roomNo;
    }

    private MemberDto getLoginUser(HttpSession session) {
        Object sessionLoginUser = session.getAttribute("loginUser");

        if (sessionLoginUser instanceof MemberDto memberDto) {
            return memberDto;
        }

        MemberDto loginUser = loginMemberResolver.getCurrentMember();

        if (loginUser != null) {
            session.setAttribute("loginUser", loginUser);
        }

        return loginUser;
    }

    private boolean canAccessRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        if (loginMemberResolver.isAdmin(loginUser)) {
            return true;
        }

        return chatRoom.getUserNo() != null && chatRoom.getUserNo().equals(loginUser.getNo());
    }

    private String redirectToUserLogin() {
        return "redirect:/member/login";
    }
}
