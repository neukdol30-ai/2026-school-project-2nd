package com.siyan1234.itproject2nd.admin.support;

/**
 * 관리자 콘솔에서 사용하는 화면(view) 값을 한 곳에서 관리합니다.
 *
 * 컨트롤러와 Thymeleaf에서 "dashboard", "chats" 같은 문자열이 흩어지지 않도록
 * 화면 코드, 상단 영문 라벨, 한글 제목을 함께 보관합니다.
 */
public enum AdminView {

    DASHBOARD("dashboard", "Admin Dashboard", "운영 현황 요약"),
    CHATS("chats", "Chat Management", "상담 관리"),
    MEMBERS("members", "Member Management", "회원 관리"),
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
