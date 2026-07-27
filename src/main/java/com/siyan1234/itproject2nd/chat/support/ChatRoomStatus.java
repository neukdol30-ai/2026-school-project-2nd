package com.siyan1234.itproject2nd.chat.support;

/** 상담방 상태 코드 비교와 화면 표시 이름 변환을 담당합니다. */
public final class ChatRoomStatus {

    public static final String OPEN = "OPEN";
    public static final String CLOSED = "CLOSED";

    private ChatRoomStatus() {
    }

    public static boolean isOpen(String status) {
        return OPEN.equals(status);
    }

    public static boolean isClosed(String status) {
        return CLOSED.equals(status);
    }

    public static String displayName(String status) {
        if (isOpen(status)) {
            return "진행 중";
        }

        if (isClosed(status)) {
            return "종료";
        }

        return "확인 필요";
    }
}
