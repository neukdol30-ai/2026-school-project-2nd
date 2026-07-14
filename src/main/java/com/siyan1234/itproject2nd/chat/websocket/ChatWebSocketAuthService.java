package com.siyan1234.itproject2nd.chat.websocket;

import com.siyan1234.itproject2nd.chat.dto.ChatRoomDto;
import com.siyan1234.itproject2nd.member.dto.CustomUserDetails;
import com.siyan1234.itproject2nd.member.dto.MemberDto;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/**
 * WebSocket 요청에서 로그인 사용자와 상담방 접근 권한을 확인하는 Service입니다.
 *
 * 클라이언트가 보낸 senderNo, viewerNo는 신뢰하지 않고
 * WebSocketSession의 Spring Security 인증 정보를 기준으로 판단합니다.
 */
@Service
public class ChatWebSocketAuthService {

    public MemberDto getLoginUser(WebSocketSession session) {
        if (session == null) {
            return null;
        }

        Object principal = session.getPrincipal();

        if (principal == null) {
            return null;
        }

        if (principal instanceof Authentication authentication) {
            return getLoginUserFromAuthentication(authentication);
        }

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMemberDto();
        }

        return null;
    }

    public boolean canAccessRoom(MemberDto loginUser, ChatRoomDto chatRoom) {
        if (loginUser == null || chatRoom == null) {
            return false;
        }

        if (isAdmin(loginUser)) {
            return true;
        }

        return chatRoom.getUserNo() != null
                && chatRoom.getUserNo().equals(loginUser.getNo());
    }

    public boolean isAdmin(MemberDto loginUser) {
        return loginUser != null && "ADMIN".equals(loginUser.getRole());
    }

    public void closeUnauthorizedSession(WebSocketSession session) throws IOException {
        if (session != null && session.isOpen()) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized chat room access"));
        }
    }

    private MemberDto getLoginUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        Object authenticationPrincipal = authentication.getPrincipal();

        if (authenticationPrincipal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getMemberDto();
        }

        if (authenticationPrincipal instanceof MemberDto memberDto) {
            return memberDto;
        }

        return null;
    }
}
