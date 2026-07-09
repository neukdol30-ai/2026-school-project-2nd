package com.siyan1234.itproject2nd.chat.kakao.controller;

import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.chat.kakao.service.KakaoNotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카카오톡 알림 테스트 Controller
 *
 * 사용:
 * - 관리자 로그인 후 /kakao/test-message 접속
 *
 * 주의:
 * - 테스트가 끝난 뒤에는 삭제하거나 관리자만 접근 가능하게 유지한다.
 */
@RestController
@RequiredArgsConstructor
public class KakaoTestController {

    private final KakaoNotifyService kakaoNotifyService;

    @GetMapping("/kakao/test-message")
    public String testMessage() {
        ChatRoomDto chatRoom = new ChatRoomDto();
        chatRoom.setRoomNo(999);
        chatRoom.setUserNo(1);
        chatRoom.setCategory("ETC");

        kakaoNotifyService.sendNewChatRoomAlert(chatRoom);

        return "카카오톡 테스트 알림 요청 완료";
    }
}