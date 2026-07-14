package com.siyan1234.itproject2nd.chat.service;

import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.config.security.LoginMemberResolver;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 채팅 접근 권한과 로그인 사용자 조회를 담당하는 공통 Service입니다.
 *
 * Controller, WebSocket, 관리자 영역에서 반복되기 쉬운 접근 권한 판단 기준을
 * 한 곳에 모아 유지보수하기 쉽게 만든 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class ChatAccessService {

    private final LoginMemberResolver loginMemberResolver;

    /**
     * 세션 또는 Spring Security 인증 정보를 기준으로 현재 로그인 사용자를 가져옵니다.
     */
    public MemberDto getLoginUser(HttpSession session) {
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

    /**
     * 상담방 접근 가능 여부를 확인합니다.
     *
     * - 관리자는 모든 상담방 접근 가능
     * - 일반 사용자는 본인의 상담방만 접근 가능
     */
    public boolean canAccessRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        if (loginMemberResolver.isAdmin(loginUser)) {
            return true;
        }

        return chatRoom.getUserNo() != null && chatRoom.getUserNo().equals(loginUser.getNo());
    }

    /**
     * 일반 사용자가 본인의 열린 상담방인지 확인합니다.
     */
    public boolean isOwnerOfOpenRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        return "OPEN".equals(chatRoom.getStatus())
                && chatRoom.getUserNo() != null
                && chatRoom.getUserNo().equals(loginUser.getNo());
    }
}
