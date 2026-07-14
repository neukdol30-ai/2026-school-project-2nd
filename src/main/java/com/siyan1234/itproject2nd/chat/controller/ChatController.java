package com.siyan1234.itproject2nd.chat.controller;

import com.siyan1234.itproject2nd.chat.service.ChatUserPageService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 사용자 1:1 채팅 화면 Controller입니다.
 *
 * 리팩토링 기준:
 * - 이 Controller는 /chat 사용자 화면 URL 매핑만 담당합니다.
 * - 실제 화면 데이터 조립은 ChatUserPageService가 담당합니다.
 * - 메시지 API는 ChatApiController로 분리했습니다.
 * - 기존 관리자 URL 호환 redirect는 ChatLegacyController로 분리했습니다.
 */
@Controller
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatUserPageService chatUserPageService;

    /** 사용자 PC 상담 홈 */
    @GetMapping
    public String chatHome(HttpSession session, Model model) {
        return chatUserPageService.showChatHome(session, model, false);
    }

    /** 사용자 모바일 상담 홈 */
    @GetMapping("/mobile")
    public String mobileChatHome(HttpSession session, Model model) {
        return chatUserPageService.showChatHome(session, model, true);
    }

    /** 최근 사용한 상담 화면으로 이동 */
    @GetMapping("/continue")
    public String continueChatView(
            @CookieValue(value = "SECONDPRO_PERSONALIZATION", required = false) String personalizationCookie,
            @CookieValue(value = "SECONDPRO_LAST_CHAT_VIEW", required = false) String lastChatView,
            HttpSession session
    ) {
        return chatUserPageService.continueChatView(personalizationCookie, lastChatView, session);
    }

    /** 사용자 PC 상담 시작 */
    @PostMapping("/start")
    public String startChat(
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return chatUserPageService.startChat(category, session, false);
    }

    /** 사용자 모바일 상담 시작 */
    @PostMapping("/mobile/start")
    public String startMobileChat(
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return chatUserPageService.startChat(category, session, true);
    }

    /** 사용자 PC 상담방 */
    @GetMapping("/room/{roomNo}")
    public String chatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        return chatUserPageService.showChatRoom(roomNo, session, model, false);
    }

    /** 사용자 모바일 상담방 */
    @GetMapping("/mobile/room/{roomNo}")
    public String mobileChatRoom(
            @PathVariable Integer roomNo,
            HttpSession session,
            Model model
    ) {
        return chatUserPageService.showChatRoom(roomNo, session, model, true);
    }

    /** 사용자 PC 상담내역 */
    @GetMapping("/history")
    public String userHistory(
            @RequestParam(defaultValue = "1") int page,
            HttpSession session,
            Model model
    ) {
        return chatUserPageService.showUserHistory(page, session, model, false);
    }

    /** 사용자 모바일 상담내역 */
    @GetMapping("/mobile/history")
    public String userMobileHistory(
            @RequestParam(defaultValue = "1") int page,
            HttpSession session,
            Model model
    ) {
        return chatUserPageService.showUserHistory(page, session, model, true);
    }

    /** 사용자 PC 문의 유형 변경 */
    @PostMapping("/{roomNo}/category")
    public String changeCategory(
            @PathVariable Integer roomNo,
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return chatUserPageService.changeCategory(roomNo, category, session, false);
    }

    /** 사용자 모바일 문의 유형 변경 */
    @PostMapping("/mobile/{roomNo}/category")
    public String changeMobileCategory(
            @PathVariable Integer roomNo,
            @RequestParam(required = false) String category,
            HttpSession session
    ) {
        return chatUserPageService.changeCategory(roomNo, category, session, true);
    }
}
