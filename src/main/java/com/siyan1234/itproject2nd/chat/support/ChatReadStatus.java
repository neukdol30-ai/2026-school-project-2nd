package com.siyan1234.itproject2nd.chat.support;

/**
 * 채팅 메시지 읽음 여부 값을 한 곳에서 관리하는 클래스입니다.
 */
public final class ChatReadStatus {

    public static final String READ = "Y";
    public static final String UNREAD = "N";

    private ChatReadStatus() {
    }

    public static String normalize(String readYn) {
        if (READ.equals(readYn) || UNREAD.equals(readYn)) {
            return readYn;
        }

        return UNREAD;
    }

    public static boolean isUnread(String readYn) {
        return UNREAD.equals(readYn);
    }
}
