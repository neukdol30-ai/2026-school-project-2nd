package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatMessageDto;
import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.kakao.service.KakaoNotifyService;
import com.siyan1234.itproject2nd.chat.service.ChatRedisService;
import com.siyan1234.itproject2nd.chat.service.ChatService;
import com.siyan1234.itproject2nd.chat.websocket.ChatHandler;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 1:1 채팅 기능 Controller
 *
 * 사용자 화면:
 * - PC 상담 홈: /chat
 * - 모바일 상담 홈: /chat/mobile
 * - PC 상담방: /chat/room/{roomNo}
 * - 모바일 상담방: /chat/mobile/room/{roomNo}
 *
 * 관리자 화면:
 * - 상담 목록: /chat/admin
 * - 상담 상세: /chat/admin/{roomNo}
 *
 * 설계 기준:
 * - 사용자 PC/모바일은 HTML/CSS를 분리한다.
 * - 실시간 채팅 JS는 /js/chat.js 공통 사용한다.
 * - 관리자 화면은 PC 운영 화면 기준으로 가독성을 개선한다.
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatHandler chatHandler;
    private final KakaoNotifyService kakaoNotifyService;

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

    /**
     * 관리자 상담 목록 화면
     *
     * 상태, 문의 유형, 키워드 검색과 페이징을 처리한다.
     */
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
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToAdminLogin();
        }

        if (!isAdmin(loginUser)) {
            return "redirect:/chat";
        }

        page = normalizePage(page);
        size = normalizeSize(size);

        int totalCount = chatService.countAdminRooms(status, category, keyword);
        int totalPage = calculateTotalPage(totalCount, size);

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

        return "chat/admin/admin-chat-list";
    }

    /**
     * 관리자 상담 목록 실시간 갱신용 JSON API
     *
     * admin-chat-list.js가 WebSocket 알림을 받은 뒤 현재 검색 조건을 유지해서 호출한다.
     */
    @ResponseBody
    @GetMapping("/admin/rooms")
    public Map<String, Object> adminRooms(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null || !isAdmin(loginUser)) {
            return emptyAdminRoomResult(size);
        }

        page = normalizePage(page);
        size = normalizeSize(size);

        int totalCount = chatService.countAdminRooms(status, category, keyword);
        int totalPage = calculateTotalPage(totalCount, size);

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

        Map<String, Object> result = new HashMap<>();
        result.put("roomList", roomList);
        result.put("status", status);
        result.put("category", category);
        result.put("keyword", keyword);
        result.put("page", page);
        result.put("size", size);
        result.put("totalCount", totalCount);
        result.put("totalPage", totalPage);
        result.put("startPageNo", startPageNo);
        result.put("endPageNo", endPageNo);

        return result;
    }

    /**
     * 관리자 상담방 상세 화면
     *
     * 관리자가 특정 상담방에 처음 입장하면 담당 관리자(adminNo)로 자동 배정한다.
     */
    @GetMapping("/admin/{roomNo}")
    public String adminChatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToAdminLogin();
        }

        if (!isAdmin(loginUser)) {
            return "redirect:/chat";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return "redirect:/chat/admin";
        }

        if (chatRoom.getAdminNo() == null) {
            chatService.assignAdmin(roomNo, loginUser.getNo());
            chatRoom = chatService.findRoomByRoomNo(roomNo);
            chatHandler.broadcastAdminListRefresh(roomNo);
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/admin/admin-chat-room";
    }

    /**
     * 채팅방 메시지 목록 조회 API
     *
     * Oracle DB 메시지와 Redis 대기 메시지를 합쳐서 생성 시간 순서로 반환한다.
     * 메시지 조회 시 현재 접속자가 상대방 메시지를 읽은 것으로 처리한다.
     */
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

        messages.sort(
                Comparator.comparing(
                        ChatMessageDto::getCreatedDate,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )
        );

        return messages;
    }

    /**
     * 초기 테스트용 HTTP 메시지 저장 API
     *
     * 현재 실제 채팅 메시지는 WebSocket(ChatHandler)을 통해 처리한다.
     */
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

    /** 상담 종료 */
    @PostMapping("/{roomNo}/close")
    public String closeRoom(
            @PathVariable Integer roomNo,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToAdminLogin();
        }

        if (!isAdmin(loginUser)) {
            return "redirect:/chat";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return "redirect:/chat/admin";
        }

        if ("CLOSED".equals(chatRoom.getStatus())) {
            return "redirect:/chat/admin/" + roomNo;
        }

        chatService.closeRoom(roomNo);

        ChatMessageDto closeMessage = new ChatMessageDto();
        closeMessage.setRoomNo(roomNo);
        closeMessage.setSenderNo(loginUser.getNo());
        closeMessage.setMessageContent("상담이 종료되었습니다.");
        closeMessage.setReadYn("N");
        closeMessage.setCreatedDate(LocalDateTime.now());

        chatRedisService.saveMessage(closeMessage);
        chatService.updateLastMessage(roomNo, "상담이 종료되었습니다.");
        chatHandler.broadcastClose(roomNo, closeMessage);
        chatHandler.broadcastAdminListRefresh(roomNo);

        return "redirect:/chat/admin";
    }

    /** 종료 상담방 단건 삭제 */
    @PostMapping("/admin/{roomNo}/delete")
    public String deleteRoomByAdmin(
            @PathVariable Integer roomNo,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToAdminLogin();
        }

        if (!isAdmin(loginUser)) {
            return "redirect:/chat";
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return "redirect:/chat/admin";
        }

        if (!"CLOSED".equals(chatRoom.getStatus())) {
            return "redirect:/chat/admin/" + roomNo;
        }

        chatRedisService.deleteMessages(roomNo);
        chatService.deleteRoom(roomNo);
        chatHandler.broadcastAdminListRefresh(roomNo);

        return "redirect:/chat/admin";
    }

    /** 종료 상담방 다중 삭제 */
    @PostMapping("/admin/rooms/delete")
    public String deleteRoomsByAdmin(
            @RequestParam(value = "roomNoList", required = false) List<Integer> roomNoList,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToAdminLogin();
        }

        if (!isAdmin(loginUser)) {
            return "redirect:/chat";
        }

        if (roomNoList == null || roomNoList.isEmpty()) {
            return "redirect:/chat/admin";
        }

        List<Integer> closedRoomNoList = new ArrayList<>();

        for (Integer roomNo : roomNoList) {
            ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

            if (chatRoom == null) {
                continue;
            }

            if (!"CLOSED".equals(chatRoom.getStatus())) {
                continue;
            }

            closedRoomNoList.add(roomNo);
        }

        if (closedRoomNoList.isEmpty()) {
            return "redirect:/chat/admin";
        }

        for (Integer roomNo : closedRoomNoList) {
            chatRedisService.deleteMessages(roomNo);
        }

        chatService.deleteClosedRooms(closedRoomNoList);
        chatHandler.broadcastAdminListRefresh(0);

        return "redirect:/chat/admin";
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

        if (mobile) {
            return "chat/mobile/chat-home";
        }

        return "chat/pc/chat-home";
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

        if (mobile) {
            return "redirect:/chat/mobile/room/" + chatRoom.getRoomNo();
        }

        return "redirect:/chat/room/" + chatRoom.getRoomNo();
    }

    private String showChatRoom(Integer roomNo, HttpSession session, Model model, boolean mobile) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        if (!canAccessRoom(loginUser, chatRoom)) {
            return mobile ? "redirect:/chat/mobile" : "redirect:/chat";
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);
        model.addAttribute("isMobile", mobile);

        if (mobile) {
            return "chat/mobile/chat-room";
        }

        return "chat/pc/chat-room";
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

        if (mobile) {
            return "redirect:/chat/mobile/room/" + roomNo;
        }

        return "redirect:/chat/room/" + roomNo;
    }

    private MemberDto getLoginUser(HttpSession session) {
        Object sessionLoginUser = session.getAttribute("loginUser");

        if (sessionLoginUser instanceof MemberDto memberDto) {
            return memberDto;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        if (!authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            MemberDto loginUser = customUserDetails.getMemberDto();
            session.setAttribute("loginUser", loginUser);

            return loginUser;
        }

        return null;
    }

    private boolean isAdmin(MemberDto loginUser) {
        return loginUser != null && "ADMIN".equals(loginUser.getRole());
    }

    private boolean canAccessRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        if (isAdmin(loginUser)) {
            return true;
        }

        return chatRoom.getUserNo() != null && chatRoom.getUserNo().equals(loginUser.getNo());
    }

    private String redirectToUserLogin() {
        return "redirect:/member/login";
    }

    private String redirectToAdminLogin() {
        return "redirect:/member/login";
    }

    private int normalizePage(int page) {
        return Math.max(page, 1);
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return 10;
        }

        return size;
    }

    private int calculateTotalPage(int totalCount, int size) {
        int totalPage = (int) Math.ceil((double) totalCount / size);
        return Math.max(totalPage, 1);
    }

    private Map<String, Object> emptyAdminRoomResult(int size) {
        Map<String, Object> result = new HashMap<>();

        result.put("roomList", List.of());
        result.put("totalCount", 0);
        result.put("totalPage", 1);
        result.put("page", 1);
        result.put("size", normalizeSize(size));
        result.put("startPageNo", 1);
        result.put("endPageNo", 1);

        return result;
    }
}
