package com.siyan1234.itproject2nd.admin.support;

/**
 * 관리자 콘솔에서 사용하는 화면(view) 값을 한 곳에서 관리합니다.
 *
 * 관리자 기능은 /admin 단일 콘솔 안에서 우측 콘텐츠 영역만 바뀌도록 구성합니다.
 */
public enum AdminView {

    DASHBOARD("dashboard", "Admin Dashboard", "운영 현황 요약"),
    MEMBERS("members", "Member Management", "회원 관리"),
    MEMBER_EDIT("memberEdit", "Member Edit", "회원 정보 수정"),
    CHATS("chats", "Chat Management", "상담 관리"),
    CHAT_ROOM("chatRoom", "Chat Room", "상담 상세"),
    BOARDS("boards", "Board Management", "게시글 관리"),
    VISITS("visits", "Visit Log", "방문 기록"),
    SERVICES("services", "Service Status", "외부 서비스 상태");

    private final String code;
    private final String eyebrow;
    private final String title;

    AdminView(String code, String eyebrow, String title) {
        this.code = code;
        this.eyebrow = eyebrow;
        this.title = title;
    }

    public String getCode() {
        return code;
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getTitle() {
        return title;
    }

    public static AdminView from(String code) {
        for (AdminView view : values()) {
            if (view.code.equals(code)) {
                return view;
            }
        }
        return DASHBOARD;
    }
}
