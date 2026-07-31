package com.siyan1234.itproject2nd.chat.websocket;

/**
 * 채팅 WebSocket에서 사용하는 이벤트 타입을 한곳에서 관리합니다.
 *
 * 서버의 수신 분기와 송신 payload가 같은 문자열을 사용하도록 하여
 * 오타로 인한 실시간 이벤트 누락을 방지합니다.
 */
public final class ChatWebSocketEventType {

    public static final String ADMIN_LIST_JOIN = "ADMIN_LIST_JOIN";
    public static final String JOIN = "JOIN";
    public static final String READ = "READ";
    public static final String MESSAGE = "MESSAGE";
    public static final String CLOSE = "CLOSE";
    public static final String ERROR = "ERROR";
    public static final String ADMIN_ROOM_REFRESH = "ADMIN_ROOM_REFRESH";

    private ChatWebSocketEventType() {
    }
}
