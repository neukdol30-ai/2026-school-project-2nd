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

import java.time.LocalDateTime;
import java.util.*;

/**
 * 1:1 채팅 기능의 요청을 처리하는 Controller
 *
 * 사용자 기능:
 * - 상담 홈 조회
 * - 상담방 생성 및 입장
 * - 메시지 목록 조회
 * - 문의 유형 변경
 *
 * 관리자 기능:
 * - 상담 목록 조회
 * - 상담 목록 실시간 갱신용 JSON 제공
 * - 상담방 상세 입장
 * - 상담 종료
 * - 종료 상담방 단건 삭제
 * - 종료 상담방 다중 삭제
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatRedisService chatRedisService;
    private final ChatHandler chatHandler;

    /**
     * 사용자 상담 홈 화면
     *
     * 사용자가 /chat에 접속했을 때 상담 홈 화면을 보여준다.
     * 진행 중인 OPEN 상담방이 있으면 openRoom으로 전달해서
     * "기존 대화 이어가기" 버튼을 화면에 표시한다.
     */
    @GetMapping
    public String chatHome(HttpSession session, Model model) {
        MemberDto loginUser = getLoginUser(session);

        // 현재는 채팅 단독 테스트를 위해 임시 로그인으로 이동
        // 팀 회원 기능과 병합 후에는 redirect:/member/login 으로 변경
        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto openRoom = chatService.findOpenRoomByUserNo(loginUser.getNo());

        model.addAttribute("loginUser", loginUser);
        model.addAttribute("openRoom", openRoom);

        return "chat/chat-home";
    }

    /**
     * 상담 시작
     *
     * 사용자가 문의 유형을 선택하면 기존 OPEN 상담방이 있는지 확인한다.
     * 기존 방이 있으면 재사용하고, 없으면 새 상담방을 생성한다.
     * 상담방 생성/문의 유형 변경 후 관리자 상담 목록을 실시간 갱신한다.
     */
    @PostMapping("/start")
    public String startChat(
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.getOrCreateRoom(loginUser.getNo(), category);

        // 새 상담방 생성 또는 문의 유형 변경 시 관리자 목록 실시간 갱신
        chatHandler.broadcastAdminListRefresh(chatRoom.getRoomNo());

        return "redirect:/chat/room/" + chatRoom.getRoomNo();
    }

    /**
     * 사용자 채팅방 화면
     *
     * 사용자가 실제 채팅방 화면에 들어갈 때 사용한다.
     * roomNo를 직접 입력해서 다른 사용자의 상담방에 접근하지 못하도록
     * 본인 상담방인지 검사한다.
     */
    @GetMapping("/room/{roomNo}")
    public String chatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return "redirect:/chat";
        }

        // 일반 사용자는 본인 상담방만 접근 가능
        if (!canAccessRoom(loginUser, chatRoom)) {
            return "redirect:/chat";
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/chat";
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

        return "chat/admin-chat-list";
    }

    /**
     * 관리자 상담 목록 실시간 갱신용 JSON API
     *
     * admin-chat-list.js에서 WebSocket 알림을 받은 뒤 fetch로 호출한다.
     * 이 API를 통해 현재 검색 조건과 페이지를 유지한 채 목록 영역만 다시 렌더링한다.
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
     * 이미 담당 관리자가 있는 경우에는 덮어쓰지 않는다.
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

        // 담당 관리자가 없는 상담방은 처음 입장한 관리자를 담당자로 배정
        if (chatRoom.getAdminNo() == null) {
            chatService.assignAdmin(roomNo, loginUser.getNo());
            chatRoom = chatService.findRoomByRoomNo(roomNo);

            chatHandler.broadcastAdminListRefresh(roomNo);
        }

        model.addAttribute("chatRoom", chatRoom);
        model.addAttribute("loginUser", loginUser);

        return "chat/admin-chat-room";
    }

    /**
     * 채팅방 메시지 목록 조회 API
     *
     * Oracle DB에 저장된 메시지와 Redis에 아직 남아있는 메시지를 합쳐서 반환한다.
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

        // DB 메시지와 Redis 메시지 모두 읽음 처리
        chatService.updateReadYn(roomNo, loginUser.getNo());
        chatRedisService.updateReadYn(roomNo, loginUser.getNo());

        List<ChatMessageDto> messages = new ArrayList<>();
        messages.addAll(chatService.findMessagesByRoomNo(roomNo));
        messages.addAll(chatRedisService.findMessages(roomNo));

        // DB 메시지와 Redis 메시지를 합친 뒤 생성 시간 기준으로 정렬
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
     * 최종 정리 시 사용하지 않으면 삭제해도 된다.
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

        chatService.saveMessage(chatMessageDto);

        return "ok";
    }

    /**
     * 상담 종료
     *
     * 관리자만 실행할 수 있다.
     * 상담방 상태를 CLOSED로 변경하고, 종료 안내 메시지를 Redis에 저장한 뒤
     * WebSocket으로 현재 채팅방과 관리자 목록에 실시간 알림을 보낸다.
     */
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

        // 관리자 목록의 마지막 메시지와 정렬 기준을 즉시 갱신
        chatService.updateLastMessage(roomNo, "상담이 종료되었습니다.");

        // 채팅방 내부에 종료 이벤트 전송
        chatHandler.broadcastClose(roomNo, closeMessage);

        // 관리자 상담 목록 실시간 갱신
        chatHandler.broadcastAdminListRefresh(roomNo);

        return "redirect:/chat/admin";
    }

    /**
     * 종료 상담방 단건 하드 DELETE
     *
     * 관리자만 실행할 수 있다.
     * Redis에 남은 메시지를 삭제하고 Oracle의 chat_room을 삭제한다.
     * chat_message는 FK ON DELETE CASCADE로 함께 삭제된다.
     */
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

    /**
     * 종료 상담방 다중 하드 DELETE
     *
     * 관리자 목록에서 체크박스로 선택한 종료 상담방을 여러 개 삭제한다.
     * 화면에서 OPEN 상담방 체크박스를 숨기더라도 서버에서 CLOSED 상태를 다시 검증한다.
     */
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

    /**
     * 문의 유형 변경
     *
     * 사용자가 문의 유형을 잘못 선택했을 때 기존 상담방은 유지하고 category만 변경한다.
     * 상담방이 OPEN 상태이고, 요청 사용자가 해당 상담방의 주인일 때만 변경된다.
     */
    @PostMapping("/{roomNo}/category")
    public String changeCategory(
            @PathVariable Integer roomNo,
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        MemberDto loginUser = getLoginUser(session);

        if (loginUser == null) {
            return redirectToUserLogin();
        }

        ChatRoomDto chatRoom = chatService.findRoomByRoomNo(roomNo);

        if (chatRoom == null) {
            return "redirect:/chat";
        }

        if (!"OPEN".equals(chatRoom.getStatus())) {
            return "redirect:/chat/room/" + roomNo;
        }

        if (chatRoom.getUserNo() == null || !chatRoom.getUserNo().equals(loginUser.getNo())) {
            return "redirect:/chat";
        }

        chatService.changeCategory(roomNo, loginUser.getNo(), category);

        chatHandler.broadcastAdminListRefresh(roomNo);

        return "redirect:/chat/room/" + roomNo;
    }

    private MemberDto getLoginUser(HttpSession session) {
        return (MemberDto) session.getAttribute("loginUser");
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
        // 최종 병합 후 변경 권장: return "redirect:/member/login";
        return "redirect:/member/login";
    }

    private String redirectToAdminLogin() {
        // 최종 병합 후 변경 권장: return "redirect:/member/login";
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