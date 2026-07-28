package com.siyan1234.itproject2nd.chat.support;

/** 채팅 메시지의 읽음 여부에 사용하는 Y/N 값과 기본값 보정 규칙을 관리합니다. */
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
