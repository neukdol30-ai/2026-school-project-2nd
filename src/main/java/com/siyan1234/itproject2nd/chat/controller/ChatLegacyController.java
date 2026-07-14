package com.siyan1234.itproject2nd.chat.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 기존 관리자 채팅 URL 호환용 Controller입니다.
 *
 * 관리자 운영 기능은 /admin 영역으로 이관했지만,
 * 기존 주소로 접근하는 경우를 위해 redirect만 유지합니다.
 */
@Controller
@RequestMapping("/chat/admin")
public class ChatLegacyController {

    @GetMapping
    public String legacyAdminChatList() {
        return "redirect:/admin?view=chats";
    }

    @GetMapping("/{roomNo}")
    public String legacyAdminChatRoom(@PathVariable Integer roomNo) {
        return "redirect:/admin/chats/" + roomNo;
    }
}
